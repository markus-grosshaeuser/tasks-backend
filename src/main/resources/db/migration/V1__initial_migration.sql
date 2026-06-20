CREATE TABLE task_lists
(
    id                   UUID                     NOT NULL,
    title                VARCHAR(255)             NOT NULL,
    description          TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_task_lists PRIMARY KEY (id)
);

CREATE TABLE tasks
(
    id           UUID                     NOT NULL,
    task_list_id UUID                     NOT NULL,
    title        VARCHAR(255)             NOT NULL,
    description  TEXT,
    due_date     TIMESTAMP WITHOUT TIME ZONE,
    position     BIGINT                   NOT NULL,
    status       VARCHAR(50)              NOT NULL,
    priority     VARCHAR(50)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id)
);

ALTER TABLE tasks
    ADD CONSTRAINT FK_TASKS_ON_TASK_LIST FOREIGN KEY (task_list_id) REFERENCES task_lists (id) ON DELETE CASCADE;

CREATE INDEX idx_tasks_on_task_list_id ON tasks (task_list_id);
CREATE INDEX idx_tasks_on_status ON tasks (status);
CREATE INDEX idx_tasks_on_priority ON tasks (priority);
CREATE INDEX idx_tasks_on_due_date ON tasks (due_date);

CREATE UNIQUE INDEX uq_tasks_task_list_id_position ON tasks(task_list_id, position);