package com.enviro.assessment.junior.chadwynprince.exception;

// Mapped to 400 by GlobalExceptionHandler.
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("'from' must not be after 'to'");
    }
}
