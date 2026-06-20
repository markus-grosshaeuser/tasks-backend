package com.grosshaeuser.tasksbackend.controllers;

import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListCreateDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListResponseDTO;
import com.grosshaeuser.tasksbackend.domain.dto.tasklist.TaskListUpdateDTO;
import com.grosshaeuser.tasksbackend.mappers.TaskListMapper;
import com.grosshaeuser.tasksbackend.services.TaskListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task-lists")
public class TaskListController {
    private final TaskListService taskListService;
    private final TaskListMapper taskListMapper;

    public TaskListController(TaskListService taskListService, TaskListMapper taskListMapper) {
        this.taskListService = taskListService;
        this.taskListMapper = taskListMapper;
    }

    @GetMapping
    public List<TaskListResponseDTO> getAllTaskLists() {
        return taskListService.getAllTaskLists().stream()
                .map(taskListMapper::toTaskListResponseDTO)
                .toList();
    }

    @GetMapping("/{taskListID}")
    public ResponseEntity<TaskListResponseDTO> getTaskListById(@PathVariable UUID taskListID) {
        return taskListService.getTaskListById(taskListID)
                .map(taskListMapper::toTaskListResponseDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskListResponseDTO> createTaskList(@Valid @RequestBody TaskListCreateDTO taskListDto) {
        TaskListResponseDTO createdTaskList = taskListMapper.toTaskListResponseDTO(taskListService.createTaskList(taskListMapper.fromTaskListCreateDTO(taskListDto)));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTaskList);
    }

    @PutMapping("/{taskListID}")
    public ResponseEntity<TaskListResponseDTO> updateTaskList(@PathVariable UUID taskListID, @Valid @RequestBody TaskListUpdateDTO taskListDto) {
        TaskListResponseDTO updatedTaskList = taskListMapper.toTaskListResponseDTO(taskListService.updateTaskList(taskListID, taskListMapper.fromTaskListUpdateDTO(taskListDto)));
        return ResponseEntity.status(HttpStatus.OK).body(updatedTaskList);
    }

    @DeleteMapping("/{taskListID}")
    public ResponseEntity<Void> deleteTaskList(@PathVariable UUID taskListID) {
        taskListService.deleteTaskList(taskListID);
        return ResponseEntity.noContent().build();
    }
}
