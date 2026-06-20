package com.grosshaeuser.tasksbackend.domain.dto.tasklist;

import jakarta.validation.constraints.NotBlank;

public record TaskListUpdateDTO(
        @NotBlank String title,
        String description
) {
}
