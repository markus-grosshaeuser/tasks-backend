package com.grosshaeuser.tasksbackend.mappers;

import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListUpdateDTO;
import com.grosshaeuser.tasksbackend.domain.entities.TaskList;

public interface TaskListMapper {
    TaskList fromTaskListCreateDTO(TaskListCreateDTO taskListDto);

    TaskList fromTaskListUpdateDTO(TaskListUpdateDTO taskListDto);

    TaskListResponseDTO toTaskListResponseDTO(TaskList taskList);
}
