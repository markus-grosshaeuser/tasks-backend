package com.grosshaeuser.tasksbackend.mappers.impl;

import com.grosshaeuser.tasksbackend.domain.dto.task.TaskCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskUpdateDTO;
import com.grosshaeuser.tasksbackend.domain.entities.Task;
import com.grosshaeuser.tasksbackend.mappers.TaskMapper;
import org.springframework.stereotype.Component;

@Component
public class TaskMapperImpl implements TaskMapper {
    @Override
    public Task fromTaskCreateDTO(TaskCreateDTO taskDto) {
        return Task.builder()
                .title(taskDto.title())
                .description(taskDto.description())
                .dueDate(taskDto.dueDate())
                .priority(taskDto.priority())
                .build();
    }

    @Override
    public Task fromTaskUpdateDTO(TaskUpdateDTO taskDto) {
        return Task.builder()
                .title(taskDto.title())
                .description(taskDto.description())
                .dueDate(taskDto.dueDate())
                .positionInList(taskDto.positionInList())
                .status(taskDto.status())
                .priority(taskDto.priority())
                .build();
    }

    @Override
    public TaskResponseDTO toTaskResponseDTO(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .positionInList(task.getPositionInList())
                .status(task.getStatus())
                .priority(task.getPriority())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}