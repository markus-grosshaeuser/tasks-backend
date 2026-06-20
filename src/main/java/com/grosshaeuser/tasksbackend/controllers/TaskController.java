package com.grosshaeuser.tasksbackend.controllers;


import com.grosshaeuser.tasksbackend.domain.dto.task.TaskCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.task.TaskUpdateDTO;
import com.grosshaeuser.tasksbackend.mappers.TaskMapper;
import com.grosshaeuser.tasksbackend.services.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task-lists/{taskListID}/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @GetMapping
    public List<TaskResponseDTO> getTasks(@PathVariable UUID taskListID) {
        return taskService.getAllTasksByTaskListId(taskListID).stream()
                .map(taskMapper::toTaskResponseDTO)
                .toList();
    }

    @GetMapping("/{taskID}")
    public ResponseEntity<TaskResponseDTO> getTask(@PathVariable UUID taskListID, @PathVariable UUID taskID) {
        return taskService.getTaskByTaskListIdAndId(taskListID, taskID)
                .map(taskMapper::toTaskResponseDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(@PathVariable UUID taskListID, @Valid @RequestBody TaskCreateDTO taskDto) {
        TaskResponseDTO createdTask = taskMapper.toTaskResponseDTO(taskService.createTask(taskListID, taskMapper.fromTaskCreateDTO(taskDto)));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{taskID}")
    public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable UUID taskListID, @PathVariable UUID taskID, @Valid @RequestBody TaskUpdateDTO taskDto) {
        TaskResponseDTO updatedTask = taskMapper.toTaskResponseDTO(taskService.updateTask(taskListID, taskID, taskMapper.fromTaskUpdateDTO(taskDto)));
        return ResponseEntity.status(HttpStatus.OK).body(updatedTask);
    }

    @DeleteMapping("/{taskID}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskListID, @PathVariable UUID taskID) {
        taskService.deleteByTaskListIdAndId(taskListID, taskID);
        return ResponseEntity.noContent().build();
    }
}
