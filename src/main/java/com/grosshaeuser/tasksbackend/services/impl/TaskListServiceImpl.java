package com.grosshaeuser.tasksbackend.services.impl;

import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import com.grosshaeuser.tasksbackend.repositories.TaskListRepo;
import com.grosshaeuser.tasksbackend.exceptions.NotFoundException;
import com.grosshaeuser.tasksbackend.services.TaskListService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskListServiceImpl implements TaskListService {

    private final TaskListRepo taskListRepo;

    public TaskListServiceImpl(TaskListRepo taskListRepo) {
        this.taskListRepo = taskListRepo;
    }

    @Override
    public List<TaskList> getAllTaskLists() {
        return taskListRepo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<TaskList> getTaskListById(UUID id) {
        return taskListRepo.findById(id);
    }

    @Override
    public TaskList createTaskList(TaskList taskList) {
        if (taskList.getId() != null) {
            throw new IllegalArgumentException("Task list ID already set.");
        }

        validateTaskListTitle(taskList);

        return taskListRepo.save(
                TaskList.builder()
                        .id(null)
                        .title(taskList.getTitle())
                        .description(taskList.getDescription())
                        .build());
    }

    @Transactional
    @Override
    public TaskList updateTaskList(UUID id, TaskList taskList) {
        if (id == null) {
            throw new IllegalArgumentException("Task list ID is required.");
        }
        if (taskList.getId() != null &&!Objects.equals(id, taskList.getId())) {
            throw new IllegalArgumentException("Task list ID is not updatable.");
        }

        validateTaskListTitle(taskList);

        TaskList persistedTaskList = taskListRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Task list not found."));

        persistedTaskList.setTitle(taskList.getTitle());
        persistedTaskList.setDescription(taskList.getDescription());
        return taskListRepo.save(persistedTaskList);
    }

    @Transactional
    @Override
    public void deleteTaskList(UUID id) {
        TaskList persistedTaskList = taskListRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Task list not found."));

        taskListRepo.delete(persistedTaskList);
    }

    private void validateTaskListTitle(TaskList taskList) {
        if (taskList.getTitle() == null || taskList.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task list title must not be blank.");
        }
    }
}
