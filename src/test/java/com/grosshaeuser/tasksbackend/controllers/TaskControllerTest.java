package com.grosshaeuser.tasksbackend.controllers;

import com.grosshaeuser.tasksbackend.TestPostgresContainer;
import com.grosshaeuser.tasksbackend.domain.entities.Task;
import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import com.grosshaeuser.tasksbackend.repositories.TaskListRepo;
import com.grosshaeuser.tasksbackend.repositories.TaskRepo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public class TaskControllerTest {
    static PostgreSQLContainer postgresContainer = TestPostgresContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    TaskListRepo taskListRepo;

    @Autowired
    TaskRepo taskRepo;

    @Autowired
    RestTestClient restTestClient;

    @BeforeAll
    public static void setUp() {
        postgresContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        postgresContainer.stop();
    }

    @BeforeEach
    public void setUpEach() {
        taskRepo.deleteAll();
        taskListRepo.deleteAll();
    }

    @Test
    public void shouldReturnAllTasksForTheProvidedTaskList() {
        TaskList workTaskList = createPersistedTaskList("Work");
        TaskList privateTaskList = createPersistedTaskList("Private");

        Task firstTask = createPersistedTask(workTaskList, "First work task", 0L);
        Task secondTask = createPersistedTask(workTaskList, "Second work task", 1L);
        createPersistedTask(privateTaskList, "Private task", 0L);

        restTestClient.get()
                .uri("/task-lists/{taskListID}/tasks", workTaskList.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(firstTask.getId().toString())
                .jsonPath("$[0].title").isEqualTo("First work task")
                .jsonPath("$[0].status").isEqualTo(TaskStatus.OPEN.name())
                .jsonPath("$[0].priority").isEqualTo(TaskPriority.LOW.name())
                .jsonPath("$[1].id").isEqualTo(secondTask.getId().toString())
                .jsonPath("$[1].title").isEqualTo("Second work task");
    }

    @Test
    public void shouldReturnAnEmptyListWhenTheTaskListHasNoTasks() {
        TaskList taskList = createPersistedTaskList("Work");

        restTestClient.get()
                .uri("/task-lists/{taskListID}/tasks", taskList.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void shouldReturnATaskWhenTheProvidedIdsMatchAnExistingTask() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Existing task", 0L);

        restTestClient.get()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(task.getId().toString())
                .jsonPath("$.title").isEqualTo("Existing task")
                .jsonPath("$.description").isEqualTo("Existing task description")
                .jsonPath("$.positionInList").isEqualTo(0)
                .jsonPath("$.status").isEqualTo(TaskStatus.OPEN.name())
                .jsonPath("$.priority").isEqualTo(TaskPriority.LOW.name())
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    public void shouldReturnNotFoundWhenTheTaskDoesNotExist() {
        TaskList taskList = createPersistedTaskList("Work");

        restTestClient.get()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldReturnNotFoundWhenTheTaskExistsButBelongsToAnotherTaskList() {
        TaskList workTaskList = createPersistedTaskList("Work");
        TaskList privateTaskList = createPersistedTaskList("Private");
        Task privateTask = createPersistedTask(privateTaskList, "Private task", 0L);

        restTestClient.get()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", workTaskList.getId(), privateTask.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldSuccessfullyCreateATask() {
        TaskList taskList = createPersistedTaskList("Work");

        Map<String, Object> requestBody = Map.of(
                "title", "Created task",
                "description", "Created task description",
                "dueDate", "2030-01-15T10:30:00",
                "priority", "HIGH"
        );

        restTestClient.post()
                .uri("/task-lists/{taskListID}/tasks", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.title").isEqualTo("Created task")
                .jsonPath("$.description").isEqualTo("Created task description")
                .jsonPath("$.dueDate").isEqualTo("2030-01-15T10:30:00")
                .jsonPath("$.positionInList").isEqualTo(0)
                .jsonPath("$.status").isEqualTo(TaskStatus.OPEN.name())
                .jsonPath("$.priority").isEqualTo(TaskPriority.HIGH.name())
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();

        assertThat(taskRepo.findAllByTaskListId(taskList.getId())).hasSize(1);
    }

    @Test
    public void shouldSuccessfullyCreateATaskWithDefaultPriorityWhenNoPriorityIsProvided() {
        TaskList taskList = createPersistedTaskList("Work");

        Map<String, Object> requestBody = Map.of(
                "title", "Created task",
                "description", "Created task description"
        );

        restTestClient.post()
                .uri("/task-lists/{taskListID}/tasks", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Created task")
                .jsonPath("$.status").isEqualTo(TaskStatus.OPEN.name())
                .jsonPath("$.priority").isEqualTo(TaskPriority.LOW.name());
    }

    @Test
    public void shouldReturnBadRequestWhenCreatingATaskWithoutTitle() {
        TaskList taskList = createPersistedTaskList("Work");

        Map<String, Object> requestBody = Map.of(
                "description", "Missing title",
                "priority", "MEDIUM"
        );

        restTestClient.post()
                .uri("/task-lists/{taskListID}/tasks", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(taskRepo.findAllByTaskListId(taskList.getId())).isEmpty();
    }

    @Test
    public void shouldReturnNotFoundWhenCreatingATaskForANonExistingTaskList() {
        Map<String, Object> requestBody = Map.of(
                "title", "Created task",
                "description", "Created task description",
                "priority", "HIGH"
        );

        restTestClient.post()
                .uri("/task-lists/{taskListID}/tasks", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(taskRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldSuccessfullyUpdateATask() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Original task", 0L);

        Map<String, Object> requestBody = Map.of(
                "title", "Updated task",
                "description", "Updated task description",
                "dueDate", "2031-02-20T12:45:00",
                "positionInList", 5,
                "status", "IN_PROGRESS",
                "priority", "MEDIUM"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(task.getId().toString())
                .jsonPath("$.title").isEqualTo("Updated task")
                .jsonPath("$.description").isEqualTo("Updated task description")
                .jsonPath("$.dueDate").isEqualTo("2031-02-20T12:45:00")
                .jsonPath("$.positionInList").isEqualTo(5)
                .jsonPath("$.status").isEqualTo(TaskStatus.IN_PROGRESS.name())
                .jsonPath("$.priority").isEqualTo(TaskPriority.MEDIUM.name());

        Task persistedTask = taskRepo.findById(task.getId()).orElseThrow();

        assertThat(persistedTask.getTitle()).isEqualTo("Updated task");
        assertThat(persistedTask.getDescription()).isEqualTo("Updated task description");
        assertThat(persistedTask.getPositionInList()).isEqualTo(5L);
        assertThat(persistedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(persistedTask.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingATaskWithoutTitle() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Original task", 0L);

        Map<String, Object> requestBody = Map.of(
                "description", "Missing title",
                "positionInList", 0,
                "status", "OPEN",
                "priority", "LOW"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        Task persistedTask = taskRepo.findById(task.getId()).orElseThrow();

        assertThat(persistedTask.getTitle()).isEqualTo("Original task");
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingATaskWithoutStatus() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Original task", 0L);

        Map<String, Object> requestBody = Map.of(
                "title", "Updated task",
                "description", "Missing status",
                "positionInList", 0,
                "priority", "LOW"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingATaskWithoutPriority() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Original task", 0L);

        Map<String, Object> requestBody = Map.of(
                "title", "Updated task",
                "description", "Missing priority",
                "positionInList", 0,
                "status", "OPEN"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingATaskThatDoesNotExist() {
        TaskList taskList = createPersistedTaskList("Work");

        Map<String, Object> requestBody = Map.of(
                "title", "Updated task",
                "description", "Updated task description",
                "positionInList", 0,
                "status", "OPEN",
                "priority", "LOW"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingATaskForANonExistingTaskList() {
        Map<String, Object> requestBody = Map.of(
                "title", "Updated task",
                "description", "Updated task description",
                "positionInList", 0,
                "status", "OPEN",
                "priority", "LOW"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", UUID.randomUUID(), UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldSuccessfullyDeleteATask() {
        TaskList taskList = createPersistedTaskList("Work");
        Task task = createPersistedTask(taskList, "Task to delete", 0L);

        restTestClient.delete()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), task.getId())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(taskRepo.existsById(task.getId())).isFalse();
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingATaskThatDoesNotExist() {
        TaskList taskList = createPersistedTaskList("Work");

        restTestClient.delete()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", taskList.getId(), UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingATaskThatBelongsToAnotherTaskList() {
        TaskList workTaskList = createPersistedTaskList("Work");
        TaskList privateTaskList = createPersistedTaskList("Private");
        Task privateTask = createPersistedTask(privateTaskList, "Private task", 0L);

        restTestClient.delete()
                .uri("/task-lists/{taskListID}/tasks/{taskID}", workTaskList.getId(), privateTask.getId())
                .exchange()
                .expectStatus().isNotFound();

        assertThat(taskRepo.existsById(privateTask.getId())).isTrue();
    }

    private TaskList createPersistedTaskList(String title) {
        return taskListRepo.save(
                TaskList.builder()
                        .title(title)
                        .description(title + " description")
                        .build()
        );
    }

    private Task createPersistedTask(TaskList taskList, String title, Long positionInList) {
        return taskRepo.save(
                Task.builder()
                        .taskList(taskList)
                        .title(title)
                        .description(title + " description")
                        .dueDate(LocalDateTime.now().plusDays(1))
                        .positionInList(positionInList)
                        .status(TaskStatus.OPEN)
                        .priority(TaskPriority.LOW)
                        .build()
        );
    }
}