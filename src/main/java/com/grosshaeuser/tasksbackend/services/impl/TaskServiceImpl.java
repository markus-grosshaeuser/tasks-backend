package com.grosshaeuser.tasksbackend.services.impl;

import com.grosshaeuser.tasksbackend.domain.entities.Task;
import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import com.grosshaeuser.tasksbackend.repositories.TaskListRepo;
import com.grosshaeuser.tasksbackend.repositories.TaskRepo;
import com.grosshaeuser.tasksbackend.exceptions.NotFoundException;
import com.grosshaeuser.tasksbackend.services.TaskService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepo taskRepo;
    private final TaskListRepo taskListRepo;

    public TaskServiceImpl(TaskRepo taskRepo, TaskListRepo taskListRepo) {
        this.taskRepo = taskRepo;
        this.taskListRepo = taskListRepo;
    }

    @Override
    public List<Task> getAllTasksByTaskListId(UUID taskListId) {
        return taskRepo.findAllByTaskListIdOrderByPositionInListAsc(taskListId);
    }

    @Override
    public Optional<Task> getTaskByTaskListIdAndId(UUID tasListId, UUID taskId) {
        return taskRepo.findByTaskListIdAndId(tasListId, taskId);
    }

    @Transactional
    @Override
    public Task createTask(UUID taskListId, Task task) {
        if (task.getId() != null) {
            throw new IllegalArgumentException("Task ID already set.");
        }

        validateTaskTitle(task);

        TaskList taskList = taskListRepo.findById(taskListId)
                .orElseThrow(() -> new NotFoundException("Task list not found."));

        return taskRepo.save(
                Task.builder()
                        .id(null)
                        .taskList(taskList)
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .dueDate(task.getDueDate())
                        .positionInList(taskRepo.countByTaskListId(taskList.getId()))
                        .status(TaskStatus.OPEN)
                        .priority(Optional.ofNullable(task.getPriority()).orElse(TaskPriority.LOW))
                        .build());
    }


    @Transactional
    @Override
    public Task updateTask(UUID taskListId, UUID taskId, Task task) {
        validateTaskTitle(task);

        TaskList taskList = taskListRepo.findById(taskListId)
                .orElseThrow(() -> new NotFoundException("Task list not found."));

        Task persistedTask = taskRepo.findByTaskListIdAndId(taskList.getId(), taskId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        persistedTask.setTitle(task.getTitle());
        persistedTask.setDescription(task.getDescription());
        persistedTask.setDueDate(task.getDueDate());
        persistedTask.setPositionInList(task.getPositionInList());
        persistedTask.setStatus(task.getStatus());
        persistedTask.setPriority(task.getPriority());
        return taskRepo.save(persistedTask);
    }

    @Transactional
    @Override
    public void deleteByTaskListIdAndId(UUID taskListId, UUID taskId) {
        Task persistedTask = taskRepo.findByTaskListIdAndId(taskListId, taskId)
                .orElseThrow(() -> new NotFoundException("Task not found."));

        taskRepo.delete(persistedTask);
    }


    private void validateTaskTitle(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank.");
        }
    }
}
