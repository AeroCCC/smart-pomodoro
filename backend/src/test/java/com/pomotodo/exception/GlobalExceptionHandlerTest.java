package com.pomotodo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStructuredErrorForApiException() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        ApiException ex = ApiException.badRequest("INVALID_INPUT", "Invalid input");

        ResponseEntity<ApiError> response = handler.handleApiException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
        assertThat(response.getBody().getPath()).isEqualTo("/api/tasks");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void shouldReturnStructuredErrorForAccessDenied() {
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/teams");

        ResponseEntity<ApiError> response = handler.handleAccessDenied(new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
        assertThat(response.getBody().getPath()).isEqualTo("/api/teams");
    }
}

