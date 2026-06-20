package com.grosshaeuser.tasksbackend.domain.dto.tasklist;

import jakarta.validation.constraints.NotBlank;

public record TaskListCreateDTO(
        @NotBlank String title,
        String description) {
}
