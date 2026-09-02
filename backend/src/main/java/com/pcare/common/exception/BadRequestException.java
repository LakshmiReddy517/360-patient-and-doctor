package com.pcare.common.exception;

/** Thrown for business-rule / validation failures. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
