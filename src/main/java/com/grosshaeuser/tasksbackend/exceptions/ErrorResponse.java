package com.grosshaeuser.tasksbackend.exceptions;

import org.springframework.http.HttpStatusCode;

public record ErrorResponse(HttpStatusCode statusCode, String message, String details) {}
