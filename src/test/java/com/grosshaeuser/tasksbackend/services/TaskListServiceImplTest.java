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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class TaskListServiceImplTest {

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
    TaskListService taskListService;

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
    public void shouldSuccessfullyCreateATaskListWhenATitleIsProvided() {
        TaskList createdTaskList = taskListService.createTaskList(
                TaskList.builder()
                        .title("Work")
                        .build()
        );

        assertThat(createdTaskList.getId()).isNotNull();
        assertThat(createdTaskList.getTitle()).isEqualTo("Work");
        assertThat(createdTaskList.getDescription()).isNull();
        assertThat(createdTaskList.getCreatedAt()).isNotNull();
        assertThat(createdTaskList.getUpdatedAt()).isNotNull();

        assertThat(taskListRepo.findById(createdTaskList.getId())).isPresent();
    }

    @Test
    public void shouldSuccessfullyCreateATaskListWhenATitleAndDescriptionAreProvided() {
        TaskList createdTaskList = taskListService.createTaskList(
                TaskList.builder()
                        .title("Work")
                        .description("Tasks related to work")
                        .build()
        );

        assertThat(createdTaskList.getId()).isNotNull();
        assertThat(createdTaskList.getTitle()).isEqualTo("Work");
        assertThat(createdTaskList.getDescription()).isEqualTo("Tasks related to work");

        TaskList persistedTaskList = taskListRepo.findById(createdTaskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Work");
        assertThat(persistedTaskList.getDescription()).isEqualTo("Tasks related to work");
    }

    @Test
    public void shouldReturnATaskListWhenTheProvidedIdMatchesAnExistingTaskList() {
        TaskList createdTaskList = createPersistedTaskList("Work", "Tasks related to work");

        assertThat(taskListService.getTaskListById(createdTaskList.getId()))
                .isPresent()
                .get()
                .satisfies(taskList -> {
                    assertThat(taskList.getId()).isEqualTo(createdTaskList.getId());
                    assertThat(taskList.getTitle()).isEqualTo("Work");
                    assertThat(taskList.getDescription()).isEqualTo("Tasks related to work");
                });
    }

    @Test
    public void shouldSuccessfullyUpdateATaskListWhenValidDataIsProvided() {
        TaskList createdTaskList = createPersistedTaskList("Work", "Original description");

        TaskList updatedTaskList = taskListService.updateTaskList(
                createdTaskList.getId(),
                TaskList.builder()
                        .id(createdTaskList.getId())
                        .title("Private")
                        .description("Updated description")
                        .build()
        );

        assertThat(updatedTaskList.getId()).isEqualTo(createdTaskList.getId());
        assertThat(updatedTaskList.getTitle()).isEqualTo("Private");
        assertThat(updatedTaskList.getDescription()).isEqualTo("Updated description");

        TaskList persistedTaskList = taskListRepo.findById(createdTaskList.getId()).orElseThrow();

        assertThat(persistedTaskList.getTitle()).isEqualTo("Private");
        assertThat(persistedTaskList.getDescription()).isEqualTo("Updated description");
    }

    @Test
    public void shouldSuccessfullyDeleteATaskListAndAllAssociatedTasksWhenTheProvidedIdMatchesAnExistingTaskList() {
        TaskList taskList = createPersistedTaskList("Work", "Tasks related to work");

        Task firstTask = createPersistedTask(taskList, "First task");
        Task secondTask = createPersistedTask(taskList, "Second task");

        taskListService.deleteTaskList(taskList.getId());

        assertThat(taskListRepo.existsById(taskList.getId())).isFalse();
        assertThat(taskRepo.existsById(firstTask.getId())).isFalse();
        assertThat(taskRepo.existsById(secondTask.getId())).isFalse();
    }

    @Test
    public void shouldFailToCreateATaskListWhenNoTitleIsProvided() {
        assertThatThrownBy(() -> taskListService.createTaskList(
                TaskList.builder()
                        .description("Missing title")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task list title must not be blank.");
    }

    @Test
    public void shouldFailToCreateATaskListWhenAPredefinedIdIsProvided() {
        assertThatThrownBy(() -> taskListService.createTaskList(
                TaskList.builder()
                        .id(UUID.randomUUID())
                        .title("Work")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task list ID already set.");
    }

    @Test
    public void shouldFailToUpdateATaskListWhenItDoesNotExist() {
        UUID taskListId = UUID.randomUUID();

        assertThatThrownBy(() -> taskListService.updateTaskList(
                taskListId,
                TaskList.builder()
                        .id(taskListId)
                        .title("Updated title")
                        .description("Updated description")
                        .build()
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task list not found.");
    }

    @Test
    public void shouldFailToUpdateATaskListWhenNoTitleIsProvided() {
        TaskList createdTaskList = createPersistedTaskList("Work", "Original description");

        assertThatThrownBy(() -> taskListService.updateTaskList(
                createdTaskList.getId(),
                TaskList.builder()
                        .id(createdTaskList.getId())
                        .description("Missing title")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task list title must not be blank.");
    }

    @Test
    public void shouldFailToDeleteATaskListWhenItDoesNotExist() {
        assertThatThrownBy(() -> taskListService.deleteTaskList(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task list not found.");
    }

    @Test
    public void shouldReturnAllTaskLists() {
        TaskList workTaskList = createPersistedTaskList("Work", "Tasks related to work");
        TaskList privateTaskList = createPersistedTaskList("Private", "Private tasks");

        List<TaskList> taskLists = taskListService.getAllTaskLists();

        assertThat(taskLists)
                .hasSize(2)
                .extracting(TaskList::getId)
                .containsExactlyInAnyOrder(workTaskList.getId(), privateTaskList.getId());
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNoTaskListMatchesTheProvidedId() {
        assertThat(taskListService.getTaskListById(UUID.randomUUID())).isEmpty();
    }

    @Test
    public void shouldFailToUpdateATaskListWhenPayloadIdDoesNotMatchPathId() {
        TaskList createdTaskList = createPersistedTaskList("Work", "Original description");

        assertThatThrownBy(() -> taskListService.updateTaskList(
                createdTaskList.getId(),
                TaskList.builder()
                        .id(UUID.randomUUID())
                        .title("Updated title")
                        .description("Updated description")
                        .build()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task list ID is not updatable.");
    }

    @Test
    public void shouldSetCreationDateWhenTaskListIsCreated() {
        TaskList createdTaskList = taskListService.createTaskList(
                TaskList.builder()
                        .title("Work")
                        .build()
        );

        assertThat(createdTaskList.getCreatedAt()).isNotNull();
        assertThat(createdTaskList.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    public void shouldSetUpdateDateToTheSameDateAsCreationDateWhenTaskListIsCreated() {
        TaskList createdTaskList = taskListService.createTaskList(
                TaskList.builder()
                        .title("Work")
                        .build()
        );

        assertThat(createdTaskList.getCreatedAt()).isNotNull();
        assertThat(createdTaskList.getUpdatedAt()).isNotNull();
        assertThat(createdTaskList.getUpdatedAt()).isEqualTo(createdTaskList.getCreatedAt());
    }

    @Test
    public void shouldSetUpdateDateWhenTaskListIsUpdated() throws InterruptedException {
        TaskList createdTaskList = taskListService.createTaskList(
                TaskList.builder()
                        .title("Work")
                        .description("Original description")
                        .build()
        );

        Instant originalUpdatedAt = createdTaskList.getUpdatedAt();

        Thread.sleep(10);

        TaskList updatedTaskList = taskListService.updateTaskList(
                createdTaskList.getId(),
                TaskList.builder()
                        .id(createdTaskList.getId())
                        .title("Updated title")
                        .description("Updated description")
                        .build()
        );

        assertThat(updatedTaskList.getUpdatedAt()).isNotNull();
        assertThat(updatedTaskList.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    private TaskList createPersistedTaskList(String title, String description) {
        return taskListRepo.save(
                TaskList.builder()
                        .title(title)
                        .description(description)
                        .build()
        );
    }

    private Task createPersistedTask(TaskList taskList, String title) {
        return taskRepo.save(
                Task.builder()
                        .taskList(taskList)
                        .title(title)
                        .description(title + " description")
                        .positionInList(taskRepo.countByTaskListId(taskList.getId()))
                        .status(TaskStatus.OPEN)
                        .priority(TaskPriority.LOW)
                        .build()
        );
    }
}