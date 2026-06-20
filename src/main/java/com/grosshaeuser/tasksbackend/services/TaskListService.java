package com.grosshaeuser.tasksbackend.services;

import com.grosshaeuser.tasksbackend.domain.entities.TaskList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskListService {
    List<TaskList> getAllTaskLists();

    Optional<TaskList> getTaskListById(UUID id);

    TaskList createTaskList(TaskList taskList);

    TaskList updateTaskList(UUID id, TaskList taskList);

    void deleteTaskList(UUID id);
}
