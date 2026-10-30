package ma.enset.aiagentservice.web.rest;

import jakarta.servlet.http.HttpServletRequest;
import ma.enset.aiagentservice.service.exception.AgentUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(AgentUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Données invalides", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
