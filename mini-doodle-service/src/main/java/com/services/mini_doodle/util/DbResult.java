package com.services.mini_doodle.util;

import lombok.Getter;

import java.util.Optional;

public class DbResult<T> {

    private final T value;
    @Getter
    private final String error;

    // Private constructor: enforce use of static factory methods
    private DbResult(T value, String error) {
        this.value = value;
        this.error = error;
    }

    // Static Factory for Success
    public static <T> DbResult<T> success(T value) {
        return new DbResult<>(value, null);
    }

    // Static Factory for Failure
    public static <T> DbResult<T> error(String errorMessage) {
        return new DbResult<>(null, errorMessage);
    }

    public boolean isSuccess() {
        return error == null;
    }

    // Returns the value wrapped in an Optional
    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

}
