package com.grosshaeuser.tasksbackend.mappers;

import com.grosshaeuser.tasksbackend.domain.dto.task.TaskCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskUpdateDTO;
import com.grosshaeuser.tasksbackend.domain.entities.Task;

public interface TaskMapper {
    Task fromTaskCreateDTO(TaskCreateDTO taskDto);

    Task fromTaskUpdateDTO(TaskUpdateDTO taskDto);

    TaskResponseDTO toTaskResponseDTO(Task task);
}
