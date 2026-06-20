package com.grosshaeuser.tasksbackend;

import org.testcontainers.postgresql.PostgreSQLContainer;

public class TestPostgresContainer extends PostgreSQLContainer {
    private static final String DOCKER_IMAGE_NAME = "postgres:18.4-alpine";

    private static TestPostgresContainer instance;

    public TestPostgresContainer() {
        super(DOCKER_IMAGE_NAME);
    }

    public static TestPostgresContainer getInstance() {
        if (instance == null) {
            instance = new TestPostgresContainer();
        }
        return instance;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("TEST_DB_URL", this.getJdbcUrl());
        System.setProperty("TEST_DB_USERNAME", this.getUsername());
        System.setProperty("TEST_DB_PASSWORD", this.getPassword());
    }

    @Override
    public void stop() {
    }

}
