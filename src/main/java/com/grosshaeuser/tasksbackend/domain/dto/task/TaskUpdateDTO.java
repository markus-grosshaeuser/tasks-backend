package com.grosshaeuser.tasksbackend.domain.dto.task;

import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskUpdateDTO(
        @NotBlank String title,
        String description,
        LocalDateTime dueDate,
        @NotNull Long positionInList,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority
) {
}
