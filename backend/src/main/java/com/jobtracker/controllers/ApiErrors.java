package com.jobtracker.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import com.jobtracker.services.TrackerException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiErrors {
    @ExceptionHandler(TrackerException.class)
    ProblemDetail workflow(TrackerException error) {
        int status = switch (error.category) {
            case INVALID -> 400;
            case MISSING -> 404;
            case CONFLICT -> 409;
        };
        return problem(status, error.code, error.getMessage(), error.fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail fields(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return problem(400, "invalid_input", "Check the highlighted fields.", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraints(ConstraintViolationException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getConstraintViolations().forEach(field -> fields.putIfAbsent(field.getPropertyPath().toString(), field.getMessage()));
        return problem(400, "invalid_input", "Check the highlighted fields.", fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail parameters(HandlerMethodValidationException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            if (name != null && !result.getResolvableErrors().isEmpty()) {
                fields.put(name, result.getResolvableErrors().getFirst().getDefaultMessage());
            }
        });
        return problem(400, "invalid_input", "Check the request parameters.", fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail types(MethodArgumentTypeMismatchException error) {
        return problem(400, "invalid_input", "Check the request parameters.", Map.of(error.getName(), "Invalid value."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, JsonProcessingException.class})
    ProblemDetail badJson(Exception error) {
        Throwable cause = error instanceof HttpMessageNotReadableException ? error.getCause() : error;
        Map<String, String> fields = new LinkedHashMap<>();
        if (cause instanceof JsonMappingException mapping) {
            StringBuilder path = new StringBuilder();
            for (var part : mapping.getPath()) {
                if (part.getFieldName() != null) {
                    if (!path.isEmpty()) path.append('.');
                    path.append(part.getFieldName());
                } else {
                    path.append('[').append(part.getIndex()).append(']');
                }
            }
            if (!path.isEmpty()) fields.put(path.toString(), "Invalid value.");
        }
        return problem(400, "invalid_json", "Send JSON with valid field names and values.", fields);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLock() {
        return problem(409, "stale_version", "This record changed since you opened it.", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail notFound() {
        return problem(404, "not_found", "This resource was not found.", Map.of());
    }

    static ProblemDetail problem(int status, String code, String detail, Map<String, String> fields) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setProperty("code", code);
        if (!fields.isEmpty()) problem.setProperty("fieldErrors", fields);
        return problem;
    }
}
