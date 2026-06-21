# Tasks Backend

![](https://github.com/markus-grosshaeuser/badges/blob/main/versions/version_1_0_1.svg)
![](https://github.com/markus-grosshaeuser/badges/blob/main/languages/Java-25.svg)
![](https://github.com/markus-grosshaeuser/badges/blob/main/frameworks/Spring_Boot-v4.0.6.svg)
![](https://github.com/markus-grosshaeuser/badges/blob/main/tools/Maven-v3.9.16.svg)
![](https://github.com/markus-grosshaeuser/badges/blob/main/dbs/PostgreSQL-v18.4.svg)
![](https://github.com/markus-grosshaeuser/badges/blob/main/license-MIT.svg)



Backend for a simple task planner application.

The application provides a REST API for managing task lists and their tasks. It is built with Spring Boot, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, and Maven.

## Features

- Create, read, update, and delete task lists
- Create, read, update, and delete tasks inside task lists
- Track task status and priority
- Store optional task descriptions and due dates
- Calculate task-list completion ratios
- Persist data in PostgreSQL
- Manage database schema with Flyway migrations
- Run integration tests with Testcontainers

## Tech Stack

- Java 25
- Spring Boot 4
- Spring MVC
- Spring Data JPA
- Jakarta Validation
- Flyway
- PostgreSQL 18
- Maven 3
- Docker Compose
- JUnit 5
- Testcontainers
- Lombok

## Requirements

Make sure the following tools are installed:

- Java 25
- Docker
- Docker Compose

You do not need to install Maven manually because the project includes the Maven Wrapper.

## Getting Started

### 1. Clone the repository

```bash 
git clone https://github.com/markus-grosshaeuser/tasks-backend.git
cd tasks-backend
```

### 2. Start the PostgreSQL database

```bash 
docker compose up -d
``` 

The default local database configuration is stored in `database.env`.

> The credentials in `database.env` are intended for local development only.

### 3. Run the application

```bash 
./mvnw spring-boot:run
```

On Windows:

```bash 
mvnw.cmd spring-boot:run
``` 

The backend starts on the configured Spring Boot port.

## Running Tests

Run the full test suite with:

```bash 
./mvnw test
```

On Windows:

```bash 
mvnw.cmd test
``` 

The tests use Testcontainers and start a PostgreSQL container automatically. Docker must be running.

## Building the Application

Create a production-ready JAR with:

```bash 
./mvnw clean package
```

On Windows:

```bash
mvnw.cmd clean package
``` 

The generated artifact is available in the `target/` directory.

Run the packaged application with:

```bash 
java -jar target/tasks-backend-1.0.0.jar
```

## API Overview

Base paths:

```text 
/task-lists 
/task-lists/{taskListId}/tasks
``` 

All request and response bodies use JSON.

## Task Lists

### Get all task lists

```http 
GET /task-lists
```

Example response:

```json
[
  {
    "id":"65f8bb6e-fdb8-4a95-8c55-4fc6f03cbb40",
    "title":"Work",
    "description":"Tasks related to work",
    "completionRatio":0.5
  }
]
``` 

### Get a task list by ID

```http 
GET /task-lists/{taskListId}
```

Successful response:

```json 
{
  "id": "65f8bb6e-fdb8-4a95-8c55-4fc6f03cbb40", 
  "title": "Work", 
  "description": "Tasks related to work", 
  "completionRatio": 0.5
}
``` 

Returns `404 Not Found` if no task list exists for the provided ID.

### Create a task list

```http 
POST /task-lists Content-Type: application/json
```

Request body:

```json 
{ 
  "title": "Work", 
  "description": "Tasks related to work"
}
``` 

`description` is optional.

Successful response status:

```text 
201 Created
```

Example response:

```json 
{ 
  "id": "65f8bb6e-fdb8-4a95-8c55-4fc6f03cbb40", 
  "title": "Work", 
  "description": "Tasks related to work", 
  "completionRatio": 0.0
}
``` 

Validation rules:

- `title` is required
- `title` must not be blank

### Update a task list

```http 
PUT /task-lists/{taskListId} Content-Type: application/json
```

Request body:

```json 
{ 
  "title": "Private", 
  "description": "Updated description"
}
``` 

`PUT` replaces the editable task-list fields. If `description` is omitted, it is stored as `null`.

Successful response:

```json 
{ 
  "id": "65f8bb6e-fdb8-4a95-8c55-4fc6f03cbb40", 
  "title": "Private", 
  "description": "Updated description", 
  "completionRatio": 0.0
}
```

Returns:

- `400 Bad Request` if the request body is invalid
- `404 Not Found` if no task list exists for the provided ID

### Delete a task list

```http 
DELETE /task-lists/{taskListId}
``` 

Successful response status:

```text 
204 No Content
```

Deleting a task list also deletes all associated tasks.

Returns `404 Not Found` if no task list exists for the provided ID.

## Tasks

Tasks belong to a task list and are accessed through the task-list-specific endpoint.

### Get all tasks of a task list

```http 
GET /task-lists/{taskListId}/tasks
``` 

Example response:

```json
[
  {
    "id": "8d84fbdb-f465-4d73-a83c-a462b65e924d", 
    "title": "Prepare release", 
    "description": "Finalize version 1.0.0", 
    "dueDate": "2026-06-20T12:00:00", 
    "positionInList": 0, 
    "status": "OPEN", 
    "priority": "HIGH"
  }
]
```

### Get a task by ID

```http 
GET /task-lists/{taskListId}/tasks/{taskId}
``` 

Example response:

```json 
{ 
  "id": "8d84fbdb-f465-4d73-a83c-a462b65e924d", 
  "title": "Prepare release", 
  "description": "Finalize version 1.0.0", 
  "dueDate": "2026-06-30T12:00:00", 
  "positionInList": 0, 
  "status": "OPEN", 
  "priority": "HIGH"
}
```

Returns `404 Not Found` if no task exists for the provided task list and task ID.

### Create a task

```http 
POST /task-lists/{taskListId}/tasks Content-Type: application/json
``` 

Request body:

```json 
{ 
  "title": "Prepare release", 
  "description": "Finalize version 1.0.0", 
  "dueDate": "2026-06-20T12:00:00", 
  "priority": "HIGH"
}
```

Optional fields:

- `description`
- `dueDate`
- `priority`

If `priority` is omitted, the task is created with the default priority (`LOW`).

New tasks are created with status `OPEN`.

Successful response status:

```text 
201 Created
``` 

Example response:

```json 
{ 
  "id": "8d84fbdb-f465-4d73-a83c-a462b65e924d", 
  "title": "Prepare release", 
  "description": "Finalize version 1.0.0", 
  "dueDate": "2026-06-20T12:00:00", 
  "positionInList": 0, 
  "status": "OPEN", 
  "priority": "HIGH" }
```

Validation rules:

- `title` is required
- `title` must not be blank

Returns:

- `400 Bad Request` if the request body is invalid
- `404 Not Found` if no task list exists for the provided ID

### Update a task

```http 
PUT /task-lists/{taskListId}/tasks/{taskId} Content-Type: application/json
``` 

Request body:

```json 
{ 
  "title": "Prepare release", 
  "description": "Finalize version 1.0.0", 
  "dueDate": "2026-06-20T12:00:00", 
  "positionInList": 0, 
  "status": "IN_PROGRESS", 
  "priority": "HIGH"
}
```

`PUT` is intended as a full update of the editable task fields.

Validation rules:

- `title` is required
- `title` must not be blank
- `status` is required
- `priority` is required

Successful response:

```json 
{ 
  "id": "8d84fbdb-f465-4d73-a83c-a462b65e924d", 
  "title": "Prepare release", 
  "description": "Finalize version 1.0.0", 
  "dueDate": "2026-06-20T12:00:00", 
  "positionInList": 0, 
  "status": "IN_PROGRESS", 
  "priority": "HIGH"
}
``` 

Returns:

- `400 Bad Request` if the request body is invalid
- `404 Not Found` if no task list or task exists for the provided IDs

### Delete a task

```http 
DELETE /task-lists/{taskListId}/tasks/{taskId}
```

Successful response status:

```text 
204 No Content
``` 

Returns `404 Not Found` if no task exists for the provided task list and task ID.

## Task Status Values

```text 
OPEN
IN_PROGRESS
COMPLETED
```

## Task Priority Values

```text 
LOW
MEDIUM
HIGH
``` 

## Error Responses

Error responses use a JSON body.

Example:

```json 
{ 
  "status": "BAD_REQUEST", 
  "message": "Task title must not be blank.", 
  "path": "uri=/task-lists/65f8bb6e-fdb8-4a95-8c55-4fc6f03cbb40/tasks"
}
```

Common status codes:

| Status            | Meaning                                    |
|-------------------|--------------------------------------------|
| `400 Bad Request` | Invalid request body or invalid input      |
| `404 Not Found`   | Requested task list or task does not exist |

## Database

The database schema is managed by Flyway.

Migration files are located in:

```text 
src/main/resources/db/migration
``` 

The initial migration creates:

- `task_lists`
- `tasks`
- indexes for task list, status, priority, and due date lookup
- a unique task position per task list
- cascading delete from task lists to tasks

## Development Notes

### Local database

Start the local database:

```bash 
docker compose up -d
```

Stop the local database:

```bash 
docker compose down
```

Remove the local database container and volumes:

```bash 
docker compose down -v
```

### Maven Wrapper

Use the Maven Wrapper included in the repository:

```bash 
./mvnw
``` 

On Windows:

```bash 
mvnw.cmd
```

## License
### MIT

MIT License

Copyright (c) 2026 Markus Großhäuser

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
