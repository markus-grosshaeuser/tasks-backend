package com.grosshaeuser.tasksbackend.domain.dto.task;

import com.grosshaeuser.tasksbackend.domain.entities.TaskPriority;
import com.grosshaeuser.tasksbackend.domain.entities.TaskStatus;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TaskResponseDTO(
        UUID id,
        String title,
        String description,
        LocalDateTime dueDate,
        Long positionInList,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt
) {
}