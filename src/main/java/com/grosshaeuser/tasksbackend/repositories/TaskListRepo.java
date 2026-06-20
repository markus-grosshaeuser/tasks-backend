package com.grosshaeuser.tasksbackend.repositories;

import com.grosshaeuser.tasksbackend.domain.entities.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskListRepo extends JpaRepository<TaskList, UUID> {

}
