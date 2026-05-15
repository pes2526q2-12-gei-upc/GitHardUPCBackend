package com.safesteps.backend;

import com.safesteps.backend.domain.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/test/uri");
    }

    @Test
    void handleNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found test");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not found test", response.getBody().getMessage());
    }

    @Test
    void handleUserForbidden() {
        UserForbiddenException ex = new UserForbiddenException("Forbidden test", "FORBIDDEN");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUserForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden test", response.getBody().getMessage());
    }

    @Test
    void handleBadRequestException() {
        BadRequestException ex = new BadRequestException("Bad request test");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBadRequestException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad request test", response.getBody().getMessage());
    }

    @Test
    void handleValidationException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objectName", "fieldName", "error message");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed for one or more fields.", response.getBody().getMessage());
    }

    @Test
    void handleJsonParseException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleJsonParseException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Comprova el format del JSON.", response.getBody().getMessage());
    }

    @Test
    void handleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("param");
        when(ex.getValue()).thenReturn("invalidValue");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El parametre 'param' te un valor no valid ('invalidValue')", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrity() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Integrity violation");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDataIntegrity(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Ja existeix un registre amb aquestes dades o hi ha un conflicte de relacions.", response.getBody().getMessage());
    }

    @Test
    void handleMissingParams() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("param", "String");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMissingParams(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Falta el parametre obligatori: param", response.getBody().getMessage());
    }

    @Test
    void handleURLNotFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleURLNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("La url que has posat no es correcte.", response.getBody().getMessage());
    }

    @Test
    void handleUserSuspended() {
        UserSuspendedException ex = new UserSuspendedException("Suspended test");
        ResponseEntity<Object> response = exceptionHandler.handleUserSuspended(ex, request);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Suspended test", body.get("message"));
    }

    @Test
    void handleUserBanned() {
        UserBannedException ex = new UserBannedException("Banned test");
        ResponseEntity<Object> response = exceptionHandler.handleUserBanned(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Banned test", body.get("message"));
    }

    @Test
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El metode utilitzat no esta suportat actualment per l'aplicacio.", response.getBody().getMessage());
    }

    @Test
    void handleException() {
        Exception ex = new Exception("Generic exception");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred. Try again later.", response.getBody().getMessage());
    }
}