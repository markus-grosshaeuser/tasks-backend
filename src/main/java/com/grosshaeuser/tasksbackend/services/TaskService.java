package com.grosshaeuser.tasksbackend.services;

import com.grosshaeuser.tasksbackend.domain.entities.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskService {
    List<Task> getAllTasksByTaskListId(UUID taskListId);
    Optional<Task> getTaskByTaskListIdAndId(UUID taskListId, UUID taskId);

    Task createTask(UUID taskList, Task task);

    Task updateTask(UUID taskListId, UUID taskId, Task task);

    void deleteByTaskListIdAndId(UUID taskListId, UUID taskId);
}
