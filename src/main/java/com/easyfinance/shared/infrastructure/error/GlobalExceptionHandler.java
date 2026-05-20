package com.easyfinance.shared.infrastructure.error;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import com.easyfinance.shared.infrastructure.observability.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorResponse> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorResponse> details = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed request body.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        FieldErrorResponse detail = new FieldErrorResponse(ex.getName(), "Invalid parameter type.");
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE", "Invalid request parameter type.", request, List.of(detail));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        FieldErrorResponse detail = new FieldErrorResponse(ex.getParameterName(), "Required request parameter is missing.");
        return build(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PARAMETER", "Required request parameter is missing.", request, List.of(detail));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "IMPORT_FILE_TOO_LARGE", "Import file is too large.", request, List.of());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedOperation(UnauthorizedOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenOperation(ForbiddenOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.code(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied.", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                code,
                message,
                request.getRequestURI(),
                correlationId(),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    private String correlationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    }
}
