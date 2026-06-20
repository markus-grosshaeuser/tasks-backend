package com.grosshaeuser.tasksbackend.mappers.impl;

import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListUpdateDTO;
import com.grosshaeuser.tasksbackend.domain.entities.Task;
import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import com.grosshaeuser.tasksbackend.mappers.TaskListMapper;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class TaskListMapperImpl implements TaskListMapper {

    public TaskListMapperImpl() {
    }

    @Override
    public TaskList fromTaskListCreateDTO(TaskListCreateDTO taskList) {
        return TaskList.builder()
                .title(taskList.title())
                .description(taskList.description())
                .tasks(null)
                .build();
    }

    @Override
    public TaskList fromTaskListUpdateDTO(TaskListUpdateDTO taskList) {
        return TaskList.builder()
                .title(taskList.title())
                .description(taskList.description())
                .build();
    }

    @Override
    public TaskListResponseDTO toTaskListResponseDTO(TaskList taskList) {
        return TaskListResponseDTO.builder()
                .id(taskList.getId())
                .title(taskList.getTitle())
                .description(taskList.getDescription())
                .completionRatio(calculateCompletionRatio(taskList.getTasks()))
                .build();
    }

    private Double calculateCompletionRatio(List<Task> taskList) {
        if (taskList == null || taskList.isEmpty()) {
            return 0.0;
        }

        return taskList.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count() / (double) taskList.size();
    }
}
