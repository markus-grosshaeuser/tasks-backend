package com.grosshaeuser.tasksbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TasksBackendApplication {

    static void main(String[] args) {
        SpringApplication.run(TasksBackendApplication.class, args);
    }

}
