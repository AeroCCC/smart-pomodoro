package com.pomotodo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class RestSecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void authenticationEntryPointShouldReturnStructured401() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("unauthorized"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.path("code").asText()).isEqualTo("NOT_AUTHENTICATED");
        assertThat(body.path("message").asText()).isEqualTo("Authentication required");
        assertThat(body.path("path").asText()).isEqualTo("/api/tasks");
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }

    @Test
    void accessDeniedHandlerShouldReturnStructured403() throws Exception {
        RestAccessDeniedHandler deniedHandler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/tasks/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("denied"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.path("code").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(body.path("message").asText()).isEqualTo("Access denied");
        assertThat(body.path("path").asText()).isEqualTo("/api/tasks/1");
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }
}
