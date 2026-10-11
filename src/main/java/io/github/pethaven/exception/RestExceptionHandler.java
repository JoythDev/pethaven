package io.github.pethaven.exception;

import io.github.pethaven.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE) // TODO: Eliminar cuando Thymeleaf se elimine
@RestControllerAdvice(annotations = RestController.class) // TODO: Eliminar `annotations` cuando Thymeleaf se elimine
public class RestExceptionHandler { // TODO: Renombrar a `GlobalExceptionHandler` cuando Thymeleaf se elimine

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalOperation(IllegalOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountDisabled(AccountDisabledException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos.", request, fieldErrors(ex.getBindingResult()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            String message = result.getResolvableErrors().stream()
                    .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido")
                    .collect(Collectors.joining(", "));
            errors.put(parameterName != null ? parameterName : "_param", message);
        });
        return build(HttpStatus.BAD_REQUEST, "La petición tiene parámetros inválidos.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            int lastDot = path.lastIndexOf('.');
            String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
            errors.put(field, violation.getMessage());
        });
        return build(HttpStatus.BAD_REQUEST, "La petición tiene parámetros inválidos.", request, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un formato inválido.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o está mal formado.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Conflicto de integridad de datos en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes (valor duplicado o referencia inválida).",
                request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return build(status, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor.", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                   Map<String, String> errors) {
        ApiErrorResponse body = ApiErrorResponse.of(status, message, request.getRequestURI(), errors);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), defaultMessage(fieldError));
        }
        for (ObjectError globalError : bindingResult.getGlobalErrors()) {
            errors.putIfAbsent("_global", defaultMessage(globalError));
        }
        return errors;
    }

    private String defaultMessage(ObjectError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
    }
}
