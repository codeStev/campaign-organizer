package com.campaignorganizer.shared.web;

import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.shared.domain.DomainException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central mapping of domain exceptions to RFC 9457 {@code application/problem+json}
 * (ADR-0009). Domain/application code throws framework-free {@link DomainException}s;
 * the web edge — and only the web edge — translates them to HTTP.
 */
@RestControllerAdvice
public class DomainExceptionAdvice {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex);
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", ex);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ProblemDetail handleAiUnavailable(AiUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "AI unavailable", ex);
    }

    /** Fallback for any other domain rule failure. */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", ex);
    }

    private ProblemDetail problem(HttpStatus status, String title, DomainException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        detail.setTitle(title);
        return detail;
    }
}
