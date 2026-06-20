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
public class TaskListControllerTest {
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
    public void shouldReturnAllTaskLists() {
        TaskList workTaskList = createPersistedTaskList("Work", "Tasks related to work");
        TaskList privateTaskList = createPersistedTaskList("Private", "Private tasks");

        restTestClient.get()
                .uri("/task-lists")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(workTaskList.getId().toString())
                .jsonPath("$[0].title").isEqualTo("Work")
                .jsonPath("$[0].description").isEqualTo("Tasks related to work")
                .jsonPath("$[0].completionRatio").isEqualTo(0.0)
                .jsonPath("$[1].id").isEqualTo(privateTaskList.getId().toString())
                .jsonPath("$[1].title").isEqualTo("Private")
                .jsonPath("$[1].description").isEqualTo("Private tasks")
                .jsonPath("$[1].completionRatio").isEqualTo(0.0);
    }

    @Test
    public void shouldReturnAnEmptyListWhenNoTaskListsExist() {
        restTestClient.get()
                .uri("/task-lists")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void shouldReturnATaskListWhenTheProvidedIdMatchesAnExistingTaskList() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        restTestClient.get()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(taskList.getId().toString())
                .jsonPath("$.title").isEqualTo("Work")
                .jsonPath("$.description").isEqualTo("Tasks related to work")
                .jsonPath("$.completionRatio").isEqualTo(0.0);
    }

    @Test
    public void shouldReturnNotFoundWhenNoTaskListMatchesTheProvidedId() {
        restTestClient.get()
                .uri("/task-lists/{taskListID}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldReturnTaskListWithCompletionRatioOfZeroWhenItHasNoTasks() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        restTestClient.get()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.completionRatio").isEqualTo(0.0);
    }

    @Test
    public void shouldReturnTaskListWithCompletionRatioBasedOnCompletedTasks() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        createPersistedTask(taskList, "Open task", 0L, TaskStatus.OPEN);
        createPersistedTask(taskList, "Completed task", 1L, TaskStatus.COMPLETED);
        createPersistedTask(taskList, "In progress task", 2L, TaskStatus.IN_PROGRESS);
        createPersistedTask(taskList, "Another completed task", 3L, TaskStatus.COMPLETED);

        restTestClient.get()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(taskList.getId().toString())
                .jsonPath("$.title").isEqualTo("Work")
                .jsonPath("$.completionRatio").isEqualTo(0.5);
    }

    @Test
    public void shouldSuccessfullyCreateATaskListWhenATitleIsProvided() {
        Map<String, Object> requestBody = Map.of(
                "title", "Work"
        );

        restTestClient.post()
                .uri("/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.title").isEqualTo("Work")
                .jsonPath("$.completionRatio").isEqualTo(0.0);

        assertThat(taskListRepo.findAll()).hasSize(1);
        assertThat(taskListRepo.findAll().getFirst().getTitle()).isEqualTo("Work");
        assertThat(taskListRepo.findAll().getFirst().getDescription()).isNull();
    }

    @Test
    public void shouldSuccessfullyCreateATaskListWhenATitleAndDescriptionAreProvided() {
        Map<String, Object> requestBody = Map.of(
                "title", "Work",
                "description", "Tasks related to work"
        );

        restTestClient.post()
                .uri("/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.title").isEqualTo("Work")
                .jsonPath("$.description").isEqualTo("Tasks related to work")
                .jsonPath("$.completionRatio").isEqualTo(0.0);

        assertThat(taskListRepo.findAll()).hasSize(1);
        assertThat(taskListRepo.findAll().getFirst().getTitle()).isEqualTo("Work");
        assertThat(taskListRepo.findAll().getFirst().getDescription()).isEqualTo("Tasks related to work");
    }

    @Test
    public void shouldReturnBadRequestWhenCreatingATaskListWithoutTitle() {
        Map<String, Object> requestBody = Map.of(
                "description", "Missing title"
        );

        restTestClient.post()
                .uri("/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(taskListRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldReturnBadRequestWhenCreatingATaskListWithBlankTitle() {
        Map<String, Object> requestBody = Map.of(
                "title", "   ",
                "description", "Blank title"
        );

        restTestClient.post()
                .uri("/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(taskListRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldSuccessfullyUpdateATaskListWhenValidDataIsProvided() {
        TaskList taskList = createPersistedTaskList("Work", "Original description");

        Map<String, Object> requestBody = Map.of(
                "title", "Private",
                "description", "Updated description"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(taskList.getId().toString())
                .jsonPath("$.title").isEqualTo("Private")
                .jsonPath("$.description").isEqualTo("Updated description")
                .jsonPath("$.completionRatio").isEqualTo(0.0);

        TaskList persistedTaskList = taskListRepo.findById(taskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Private");
        assertThat(persistedTaskList.getDescription()).isEqualTo("Updated description");
    }

    @Test
    public void shouldSuccessfullyUpdateATaskListDescriptionToNullWhenNoDescriptionIsProvided() {
        TaskList taskList = createPersistedTaskList("Work", "Original description");

        Map<String, Object> requestBody = Map.of(
                "title", "Work without description"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(taskList.getId().toString())
                .jsonPath("$.title").isEqualTo("Work without description");

        TaskList persistedTaskList = taskListRepo.findById(taskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Work without description");
        assertThat(persistedTaskList.getDescription()).isNull();
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingATaskListWithoutTitle() {
        TaskList taskList = createPersistedTaskList("Work", "Original description");

        Map<String, Object> requestBody = Map.of(
                "description", "Missing title"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        TaskList persistedTaskList = taskListRepo.findById(taskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Work");
        assertThat(persistedTaskList.getDescription()).isEqualTo("Original description");
    }

    @Test
    public void shouldReturnBadRequestWhenUpdatingATaskListWithBlankTitle() {
        TaskList taskList = createPersistedTaskList("Work", "Original description");

        Map<String, Object> requestBody = Map.of(
                "title", "   ",
                "description", "Blank title"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();

        TaskList persistedTaskList = taskListRepo.findById(taskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Work");
        assertThat(persistedTaskList.getDescription()).isEqualTo("Original description");
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingATaskListThatDoesNotExist() {
        Map<String, Object> requestBody = Map.of(
                "title", "Updated title",
                "description", "Updated description"
        );

        restTestClient.put()
                .uri("/task-lists/{taskListID}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void shouldSuccessfullyDeleteATaskListWhenTheProvidedIdMatchesAnExistingTaskList() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        restTestClient.delete()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(taskListRepo.existsById(taskList.getId())).isFalse();
    }

    @Test
    public void shouldSuccessfullyDeleteATaskListAndAllAssociatedTasksWhenTheProvidedIdMatchesAnExistingTaskList() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        Task firstTask = createPersistedTask(taskList, "First task", 0L, TaskStatus.OPEN);
        Task secondTask = createPersistedTask(taskList, "Second task", 1L, TaskStatus.COMPLETED);

        restTestClient.delete()
                .uri("/task-lists/{taskListID}", taskList.getId())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(taskListRepo.existsById(taskList.getId())).isFalse();
        assertThat(taskRepo.existsById(firstTask.getId())).isFalse();
        assertThat(taskRepo.existsById(secondTask.getId())).isFalse();
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingATaskListThatDoesNotExist() {
        restTestClient.delete()
                .uri("/task-lists/{taskListID}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    private TaskList createPersistedTaskList(String title, String description) {
        return taskListRepo.save(
                TaskList.builder()
                        .title(title)
                        .description(description)
                        .build()
        );
    }

    private Task createPersistedTask(TaskList taskList, String title, Long positionInList, TaskStatus status) {
        return taskRepo.save(
                Task.builder()
                        .taskList(taskList)
                        .title(title)
                        .description(title + " description")
                        .dueDate(LocalDateTime.now().plusDays(1))
                        .positionInList(positionInList)
                        .status(status)
                        .priority(TaskPriority.LOW)
                        .build()
        );
    }
}