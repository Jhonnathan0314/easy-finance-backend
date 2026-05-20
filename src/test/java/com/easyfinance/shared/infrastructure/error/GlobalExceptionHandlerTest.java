package com.easyfinance.shared.infrastructure.error;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsBusinessRuleViolationToUnprocessableEntity() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBusinessRule(
                new BusinessRuleViolationException("BUSINESS_RULE", "Business rule failed."),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("BUSINESS_RULE");
    }

    @Test
    void mapsNotFoundToNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new NotFoundException("RESOURCE_NOT_FOUND", "Resource not found."),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void mapsForbiddenOperationToForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleForbiddenOperation(
                new ForbiddenOperationException("FORBIDDEN_OPERATION", "Forbidden operation."),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN_OPERATION");
    }

    @Test
    void mapsUnauthorizedOperationToUnauthorized() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnauthorizedOperation(
                new UnauthorizedOperationException("INVALID_CREDENTIALS", "Invalid email or password."),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void mapsMalformedJsonToBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnreadableMessage(
                new HttpMessageNotReadableException("Malformed JSON", new MockHttpInputMessage(new byte[0])),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_JSON");
    }

    @Test
    void mapsArgumentTypeMismatchToBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleArgumentTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Long.class, "accountId", null, new IllegalArgumentException()),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_PARAMETER_TYPE");
        assertThat(response.getBody().details()).containsExactly(new FieldErrorResponse("accountId", "Invalid parameter type."));
    }

    @Test
    void mapsMissingRequestParameterToBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("from", "LocalDate"),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUEST_PARAMETER");
        assertThat(response.getBody().details()).containsExactly(new FieldErrorResponse("from", "Required request parameter is missing."));
    }

    @Test
    void mapsMaxUploadSizeExceededToImportFileTooLarge() {
        ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(5_242_880L),
                request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("IMPORT_FILE_TOO_LARGE");
    }

    private HttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/test");
    }
}
