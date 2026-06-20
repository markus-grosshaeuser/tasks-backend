package com.grosshaeuser.tasksbackend.db;

import com.grosshaeuser.tasksbackend.TestPostgresContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class FlyWayMigrationTest {

    static PostgreSQLContainer postgresContainer = TestPostgresContainer.getInstance();

    @BeforeAll
    public static void setUp() {
        postgresContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        postgresContainer.stop();
    }

    @Test
    public void shouldSuccessfullyApplyAllFlywayMigrations() {
        Flyway flyway = createFlyway();

        assertThatCode(flyway::migrate).doesNotThrowAnyException();

        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().current().getDescription()).isEqualTo("initial migration");
    }

    @Test
    public void shouldCreateTaskListsTableWithExpectedColumns() throws SQLException {
        migrateDatabase();

        try (Connection connection = createConnection()) {
            assertThat(tableExists(connection, "task_lists")).isTrue();

            assertThat(columnExists(connection, "task_lists", "id", "uuid")).isTrue();
            assertThat(columnExists(connection, "task_lists", "title", "character varying")).isTrue();
            assertThat(columnExists(connection, "task_lists", "description", "text")).isTrue();
            assertThat(columnExists(connection, "task_lists", "created_at", "timestamp with time zone")).isTrue();
            assertThat(columnExists(connection, "task_lists", "updated_at", "timestamp with time zone")).isTrue();

            assertThat(columnIsNullable(connection, "task_lists", "id")).isFalse();
            assertThat(columnIsNullable(connection, "task_lists", "title")).isFalse();
            assertThat(columnIsNullable(connection, "task_lists", "description")).isTrue();
            assertThat(columnIsNullable(connection, "task_lists", "created_at")).isFalse();
            assertThat(columnIsNullable(connection, "task_lists", "updated_at")).isFalse();

            assertThat(primaryKeyExists(connection, "task_lists", "pk_task_lists")).isTrue();
        }
    }

    @Test
    public void shouldCreateTasksTableWithExpectedColumns() throws SQLException {
        migrateDatabase();

        try (Connection connection = createConnection()) {
            assertThat(tableExists(connection, "tasks")).isTrue();

            assertThat(columnExists(connection, "tasks", "id", "uuid")).isTrue();
            assertThat(columnExists(connection, "tasks", "task_list_id", "uuid")).isTrue();
            assertThat(columnExists(connection, "tasks", "title", "character varying")).isTrue();
            assertThat(columnExists(connection, "tasks", "description", "text")).isTrue();
            assertThat(columnExists(connection, "tasks", "due_date", "timestamp without time zone")).isTrue();
            assertThat(columnExists(connection, "tasks", "position", "bigint")).isTrue();
            assertThat(columnExists(connection, "tasks", "status", "character varying")).isTrue();
            assertThat(columnExists(connection, "tasks", "priority", "character varying")).isTrue();
            assertThat(columnExists(connection, "tasks", "created_at", "timestamp with time zone")).isTrue();
            assertThat(columnExists(connection, "tasks", "updated_at", "timestamp with time zone")).isTrue();

            assertThat(columnIsNullable(connection, "tasks", "id")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "task_list_id")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "title")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "description")).isTrue();
            assertThat(columnIsNullable(connection, "tasks", "due_date")).isTrue();
            assertThat(columnIsNullable(connection, "tasks", "position")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "status")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "priority")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "created_at")).isFalse();
            assertThat(columnIsNullable(connection, "tasks", "updated_at")).isFalse();

            assertThat(primaryKeyExists(connection, "tasks", "pk_tasks")).isTrue();
        }
    }

    @Test
    public void shouldCreateForeignKeyFromTasksToTaskListsWithCascadeDelete() throws SQLException {
        migrateDatabase();

        try (Connection connection = createConnection()) {
            assertThat(foreignKeyExists(connection, "tasks", "fk_tasks_on_task_list")).isTrue();
            assertThat(foreignKeyHasCascadeDelete(connection, "tasks", "fk_tasks_on_task_list")).isTrue();
        }
    }

    @Test
    public void shouldCreateExpectedIndexes() throws SQLException {
        migrateDatabase();

        try (Connection connection = createConnection()) {
            assertThat(indexExists(connection, "idx_tasks_on_task_list_id")).isTrue();
            assertThat(indexExists(connection, "idx_tasks_on_status")).isTrue();
            assertThat(indexExists(connection, "idx_tasks_on_priority")).isTrue();
            assertThat(indexExists(connection, "idx_tasks_on_due_date")).isTrue();
            assertThat(indexExists(connection, "uq_tasks_task_list_id_position")).isTrue();
            assertThat(indexIsUnique(connection, "uq_tasks_task_list_id_position")).isTrue();
        }
    }

    @Test
    public void shouldEnforceCascadeDeleteFromTaskListsToTasks() throws SQLException {
        migrateDatabase();

        UUID taskListId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        try (Connection connection = createConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO task_lists (id, title, description, created_at, updated_at)
                    VALUES ('%s', 'Work', 'Tasks related to work', NOW(), NOW())
                    """.formatted(taskListId));

            connection.createStatement().executeUpdate("""
                    INSERT INTO tasks (
                        id,
                        task_list_id,
                        title,
                        description,
                        due_date,
                        position,
                        status,
                        priority,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        '%s',
                        '%s',
                        'Task',
                        'Task description',
                        NULL,
                        0,
                        'OPEN',
                        'LOW',
                        NOW(),
                        NOW()
                    )
                    """.formatted(taskId, taskListId));

            connection.createStatement().executeUpdate("""
                    DELETE FROM task_lists
                    WHERE id = '%s'
                    """.formatted(taskListId));

            assertThat(rowExists(connection, "task_lists", taskListId)).isFalse();
            assertThat(rowExists(connection, "tasks", taskId)).isFalse();
        }
    }

    @Test
    public void shouldEnforceUniquePositionPerTaskList() throws SQLException {
        migrateDatabase();

        UUID taskListId = UUID.randomUUID();
        UUID firstTaskId = UUID.randomUUID();
        UUID secondTaskId = UUID.randomUUID();

        try (Connection connection = createConnection()) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO task_lists (id, title, description, created_at, updated_at)
                    VALUES ('%s', 'Work', 'Tasks related to work', NOW(), NOW())
                    """.formatted(taskListId));

            connection.createStatement().executeUpdate("""
                    INSERT INTO tasks (
                        id,
                        task_list_id,
                        title,
                        description,
                        due_date,
                        position,
                        status,
                        priority,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        '%s',
                        '%s',
                        'First task',
                        'First task description',
                        NULL,
                        0,
                        'OPEN',
                        'LOW',
                        NOW(),
                        NOW()
                    )
                    """.formatted(firstTaskId, taskListId));

            assertThatCode(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO tasks (
                        id,
                        task_list_id,
                        title,
                        description,
                        due_date,
                        position,
                        status,
                        priority,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        '%s',
                        '%s',
                        'Second task',
                        'Second task description',
                        NULL,
                        0,
                        'OPEN',
                        'LOW',
                        NOW(),
                        NOW()
                    )
                    """.formatted(secondTaskId, taskListId)))
                    .isInstanceOf(SQLException.class);
        }
    }

    private void migrateDatabase() {
        createFlyway().migrate();
    }

    private Flyway createFlyway() {
        return Flyway.configure()
                .dataSource(
                        postgresContainer.getJdbcUrl(),
                        postgresContainer.getUsername(),
                        postgresContainer.getPassword()
                )
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword()
        );
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName, String dataType) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
                  AND data_type = '%s'
                """.formatted(tableName, columnName, dataType))) {
            return resultSet.next();
        }
    }

    private boolean columnIsNullable(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
                """.formatted(tableName, columnName))) {
            assertThat(resultSet.next()).isTrue();
            return "YES".equals(resultSet.getString("is_nullable"));
        }
    }

    private boolean primaryKeyExists(Connection connection, String tableName, String constraintName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT 1
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND constraint_name = '%s'
                  AND constraint_type = 'PRIMARY KEY'
                """.formatted(tableName, constraintName))) {
            return resultSet.next();
        }
    }

    private boolean foreignKeyExists(Connection connection, String tableName, String constraintName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT 1
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND LOWER(constraint_name) = '%s'
                  AND constraint_type = 'FOREIGN KEY'
                """.formatted(tableName, constraintName))) {
            return resultSet.next();
        }
    }

    private boolean foreignKeyHasCascadeDelete(Connection connection, String tableName, String constraintName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT rc.delete_rule
                FROM information_schema.referential_constraints rc
                JOIN information_schema.table_constraints tc
                  ON rc.constraint_schema = tc.constraint_schema
                 AND rc.constraint_name = tc.constraint_name
                WHERE tc.table_schema = 'public'
                  AND tc.table_name = '%s'
                  AND LOWER(tc.constraint_name) = '%s'
                """.formatted(tableName, constraintName))) {
            assertThat(resultSet.next()).isTrue();
            return "CASCADE".equals(resultSet.getString("delete_rule"));
        }
    }

    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT 1
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = '%s'
                """.formatted(indexName))) {
            return resultSet.next();
        }
    }

    private boolean indexIsUnique(Connection connection, String indexName) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT i.indisunique
                FROM pg_class c
                JOIN pg_index i ON c.oid = i.indexrelid
                WHERE c.relname = '%s'
                """.formatted(indexName))) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getBoolean("indisunique");
        }
    }

    private boolean rowExists(Connection connection, String tableName, UUID id) throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("""
                SELECT 1
                FROM %s
                WHERE id = '%s'
                """.formatted(tableName, id))) {
            return resultSet.next();
        }
    }
}