package com.grosshaeuser.tasksbackend.services;

import com.grosshaeuser.tasksbackend.TestPostgresContainer;
import com.grosshaeuser.tasksbackend.domain.entities.Task;
import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import com.grosshaeuser.tasksbackend.exceptions.NotFoundException;
import com.grosshaeuser.tasksbackend.repositories.TaskListRepo;
import com.grosshaeuser.tasksbackend.repositories.TaskRepo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class TaskServiceImplTest {
    static PostgreSQLContainer postgresContainer = TestPostgresContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    TaskService taskService;

    @Autowired
    TaskRepo taskRepo;

    @Autowired
    TaskListRepo taskListRepo;

    @BeforeAll
    public static void setUp() {
        postgresContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        postgresContainer.stop();
    }

    @BeforeEach
    void cleanUp() {
        taskRepo.deleteAll();
        taskListRepo.deleteAll();
    }

    @Test
    public void shouldSuccessfullyCreateATaskAndAttachItToTheCorrectTaskList() {
        TaskList taskList = createPersistedTaskList("Work");

        Task task = Task.builder()
                .title("Create service tests")
                .description("Write integration tests for task service")
                .dueDate(LocalDateTime.now().plusDays(1))
                .priority(TaskPriority.MEDIUM)
                .build();

        Task createdTask = taskService.createTask(taskList.getId(), task);

        assertThat(createdTask.getId()).isNotNull();
        assertThat(createdTask.getTaskList()).isNotNull();
        assertThat(createdTask.getTaskList().getId()).isEqualTo(taskList.getId());
        assertThat(createdTask.getTitle()).isEqualTo("Create service tests");
        assertThat(createdTask.getDescription()).isEqualTo("Write integration tests for task service");
        assertThat(createdTask.getPriority()).isEqualTo(TaskPriority.MEDIUM);

        List<Task> tasks = taskService.getAllTasksByTaskListId(taskList.getId());

        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getId()).isEqualTo(createdTask.getId());
    }

    @Test
    public void shouldSuccessfullySetTheDefaultStatusOfATask() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Task with default status")
                        .priority(TaskPriority.HIGH)
                        .build()
        );

        assertThat(createdTask.getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    public void shouldSuccessfullySetTheDefaultPriorityOfATask() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Task with default priority")
                        .build()
        );

        assertThat(createdTask.getPriority()).isEqualTo(TaskPriority.LOW);
    }

    @Test
    public void shouldSuccessfullySetTheCreationDateWhenATaskIsCreated() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Task with creation date")
                        .build()
        );

        assertThat(createdTask.getCreatedAt()).isNotNull();
        assertThat(createdTask.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    public void shouldSuccessfullySetTheUpdateDateToTheSameDateAsTheCreationDateWhenATaskIsCreated() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Task with update date")
                        .build()
        );

        assertThat(createdTask.getCreatedAt()).isNotNull();
        assertThat(createdTask.getUpdatedAt()).isNotNull();
        assertThat(createdTask.getUpdatedAt()).isEqualTo(createdTask.getCreatedAt());
    }

    @Test
    public void shouldSuccessfullySetTheUpdateDateWhenATaskIsUpdated() throws InterruptedException {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Original title")
                        .build()
        );

        Instant originalUpdatedAt = createdTask.getUpdatedAt();

        Thread.sleep(10);

        Task updatedTask = taskService.updateTask(
                taskList.getId(),
                createdTask.getId(),
                Task.builder()
                        .title("Updated title")
                        .description("Updated description")
                        .dueDate(LocalDateTime.now().plusDays(2))
                        .positionInList(createdTask.getPositionInList())
                        .status(TaskStatus.IN_PROGRESS)
                        .priority(TaskPriority.HIGH)
                        .build()
        );

        assertThat(updatedTask.getUpdatedAt()).isNotNull();
        assertThat(updatedTask.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    public void shouldSuccessfullyUpdateATaskWhenValidDataIsProvided() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Original title")
                        .description("Original description")
                        .priority(TaskPriority.LOW)
                        .build()
        );

        LocalDateTime dueDate = LocalDateTime.now().plusWeeks(1);

        Task updatedTask = taskService.updateTask(
                taskList.getId(),
                createdTask.getId(),
                Task.builder()
                        .title("Updated title")
                        .description("Updated description")
                        .dueDate(dueDate)
                        .positionInList(5L)
                        .status(TaskStatus.COMPLETED)
                        .priority(TaskPriority.HIGH)
                        .build()
        );

        assertThat(updatedTask.getId()).isEqualTo(createdTask.getId());
        assertThat(updatedTask.getTaskList().getId()).isEqualTo(taskList.getId());
        assertThat(updatedTask.getTitle()).isEqualTo("Updated title");
        assertThat(updatedTask.getDescription()).isEqualTo("Updated description");
        assertThat(updatedTask.getDueDate()).isEqualTo(dueDate);
        assertThat(updatedTask.getPositionInList()).isEqualTo(5L);
        assertThat(updatedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(updatedTask.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    public void shouldSuccessfullyDeleteATaskWhenTheProvidedIdMatchesAnExistingTask() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Task to delete")
                        .build()
        );

        taskService.deleteByTaskListIdAndId(taskList.getId(), createdTask.getId());

        assertThat(taskService.getTaskByTaskListIdAndId(taskList.getId(), createdTask.getId())).isEmpty();
        assertThat(taskRepo.existsById(createdTask.getId())).isFalse();
    }

    @Test
    public void shouldFailToCreateATaskWhenNoTitleIsProvided() {
        TaskList taskList = createPersistedTaskList("Work");

        assertThatThrownBy(() -> taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .description("Missing title")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task title must not be blank.");
    }

    @Test
    public void shouldFailToUpdateATaskWhenItDoesNotExist() {
        TaskList taskList = createPersistedTaskList("Work");

        assertThatThrownBy(() -> taskService.updateTask(
                taskList.getId(),
                UUID.randomUUID(),
                Task.builder()
                        .title("Updated title")
                        .positionInList(0L)
                        .status(TaskStatus.OPEN)
                        .priority(TaskPriority.LOW)
                        .build()
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task not found.");
    }

    @Test
    public void shouldFailToUpdateATaskWhenNoTitleIsProvided() {
        TaskList taskList = createPersistedTaskList("Work");

        Task createdTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Original title")
                        .build()
        );

        assertThatThrownBy(() -> taskService.updateTask(
                taskList.getId(),
                createdTask.getId(),
                Task.builder()
                        .description("Missing title")
                        .positionInList(createdTask.getPositionInList())
                        .status(TaskStatus.OPEN)
                        .priority(TaskPriority.LOW)
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task title must not be blank.");
    }

    @Test
    public void shouldFailToDeleteATaskWhenItDoesNotExist() {
        TaskList taskList = createPersistedTaskList("Work");

        assertThatThrownBy(() -> taskService.deleteByTaskListIdAndId(taskList.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task not found.");
    }

    @Test
    public void shouldFailToCreateATaskWhenTaskListDoesNotExist() {
        assertThatThrownBy(() -> taskService.createTask(
                UUID.randomUUID(),
                Task.builder()
                        .title("Task without existing task list")
                        .build()
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task list not found.");
    }

    @Test
    public void shouldFailToCreateATaskWhenAPredefinedIdIsProvided() {
        TaskList taskList = createPersistedTaskList("Work");

        assertThatThrownBy(() -> taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .id(UUID.randomUUID())
                        .title("Task with predefined ID")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task ID already set.");
    }

    @Test
    public void shouldReturnOnlyTasksForTheProvidedTaskListId() {
        TaskList firstTaskList = createPersistedTaskList("Work");
        TaskList secondTaskList = createPersistedTaskList("Private");

        Task firstTask = taskService.createTask(
                firstTaskList.getId(),
                Task.builder()
                        .title("Work task")
                        .build()
        );

        taskService.createTask(
                secondTaskList.getId(),
                Task.builder()
                        .title("Private task")
                        .build()
        );

        List<Task> tasks = taskService.getAllTasksByTaskListId(firstTaskList.getId());

        assertThat(tasks)
                .hasSize(1)
                .extracting(Task::getId)
                .containsExactly(firstTask.getId());
    }

    @Test
    public void shouldAssignPositionBasedOnExistingTasksInTaskList() {
        TaskList taskList = createPersistedTaskList("Work");

        Task firstTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("First task")
                        .build()
        );

        Task secondTask = taskService.createTask(
                taskList.getId(),
                Task.builder()
                        .title("Second task")
                        .build()
        );

        assertThat(firstTask.getPositionInList()).isZero();
        assertThat(secondTask.getPositionInList()).isEqualTo(1L);
    }

    private TaskList createPersistedTaskList(String title) {
        return taskListRepo.save(
                TaskList.builder()
                        .title(title)
                        .description(title + " description")
                        .build()
        );
    }
}