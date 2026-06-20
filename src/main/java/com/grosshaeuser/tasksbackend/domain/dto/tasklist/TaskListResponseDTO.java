package com.grosshaeuser.tasksbackend.domain.dto.tasklist;

import com.grosshaeuser.tasksbackend.domain.entities.Task;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record TaskListResponseDTO(
        UUID id,
        String title,
        String description,
        List<Task> tasks,
        Double completionRatio,
        Instant createdAt,
        Instant updatedAt
) {
}
