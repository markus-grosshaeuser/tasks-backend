package com.grosshaeuser.tasksbackend.repositories;

import com.grosshaeuser.tasksbackend.domain.entities.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepo extends JpaRepository<Task, UUID> {
    List<Task> findAllByTaskListIdOrderByPositionInListAsc(UUID taskListId);
    Optional<Task> findByTaskListIdAndId(UUID taskListId, UUID taskId);
    Long countByTaskListId(UUID taskListId);
}
