package com.achmadrizkin.splitbill.api.error;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Every error is an RFC 9457 Problem Details document with an extra, stable {@code error_code}.
 * Internal details (stack traces, SQL) are logged, never returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case GROUP_NOT_FOUND, EXPENSE_NOT_FOUND, PAYMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_PARTICIPANT, EXPENSE_ALREADY_VOIDED, IDEMPOTENCY_KEY_REUSED -> HttpStatus.CONFLICT;
            case VALIDATION_ERROR, INVALID_CURRENCY -> HttpStatus.BAD_REQUEST;
            case INVALID_AMOUNT, AMOUNT_SCALE_EXCEEDED, CURRENCY_MISMATCH, INVALID_SPLIT, SPLIT_SUM_MISMATCH,
                 PARTICIPANT_NOT_IN_GROUP, INVALID_PAYMENT -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Object> handleDomain(DomainException ex, WebRequest request) {
        return problem(statusOf(ex.errorCode()), ex.errorCode().name(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleConcurrentUpdate(OptimisticLockingFailureException ex, WebRequest request) {
        log.info("Concurrent modification rejected: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The group was modified by another request at the same time. Please retry.", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "CONFLICT",
                "The request conflicts with existing data. Please retry.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", request, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", toSnakeCase(fe.getField()),
                        "message", Objects.requireNonNullElse(fe.getDefaultMessage(), "is invalid")))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(),
                "Request validation failed.", request, Map.of("errors", errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing, malformed, or contains unknown or invalid fields.", request, Map.of());
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String errorCode, String detail,
                                                  WebRequest request, Map<String, Object> extra) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        body.setProperty("error_code", errorCode);
        extra.forEach(body::setProperty);
        if (request instanceof ServletWebRequest servletRequest) {
            body.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return ResponseEntity.status(status).body(body);
    }

    private static String toSnakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
