package com.grosshaeuser.tasksbackend.domain.dto.task;

import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TaskCreateDTO(
        @NotBlank String title,
        String description,
        LocalDateTime dueDate,
        TaskPriority priority
) {
}
