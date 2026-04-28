package com.safesteps.backend.domain.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    // recurs no trobat
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(ResourceNotFoundException ex, HttpServletRequest req) {
        logger.warn("Resource not found at {} - {}", req.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }
    // bad request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest req) {
        logger.warn("Bad request at {} - {}", req.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }
    // @valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorResponse err = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for one or more fields.",
                req.getRequestURI(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    // mal format de json
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonParseException(HttpMessageNotReadableException ex, HttpServletRequest req) {
        logger.warn("Malformed JSON at {} - {}", req.getRequestURI(), ex.getMessage());
        String userMessage = "Comprova el format del JSON.";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, userMessage, req);
    }

    // error a url
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = String.format("El parametre '%s' te un valor no valid ('%s')", ex.getName(), ex.getValue());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, req);
    }

    // restriccions a bd
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        logger.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = "Ja existeix un registre amb aquestes dades o hi ha un conflicte de relacions.";
        return buildErrorResponse(HttpStatus.CONFLICT, message, req);
    }

    // falta de parametres
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String message = String.format("Falta el parametre obligatori: %s", ex.getParameterName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, req);
    }

    // falta de parametres
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleURLNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        logger.warn("URL not found at {} - {}", req.getRequestURI(), ex.getMessage());
        String message = "La url que has posat no es correcte.";
        return buildErrorResponse(HttpStatus.NOT_FOUND, message, req);
    }

    // metode no suportat
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        logger.warn("Method not supported at {} - {}", req.getRequestURI(), ex.getMethod());
        String message = "El metode utilitzat no esta suportat actualment per l'aplicacio.";
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, req);
    }

    // errors no controlats
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest req) {
        logger.error("Critical error at: {}" , req.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Try again later.", req);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, HttpServletRequest req) {
        ApiErrorResponse err = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                null
        );
        return new ResponseEntity<>(err, status);
    }
}