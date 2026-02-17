package dev.horoz.url_shortener.controller.advice;

import dev.horoz.url_shortener.exceptions.EmailAlreadyExistsException;
import dev.horoz.url_shortener.exceptions.InvalidTargetUrlException;
import dev.horoz.url_shortener.exceptions.SlugAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // ======================================================================
    // Custom (domain) exceptions
    // ======================================================================

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "Email already exists", ex.getMessage(), req,
                "errorCode", "EMAIL_ALREADY_EXISTS");
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ProblemDetail handleSlugExists(SlugAlreadyExistsException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "Slug already exists",
                "The provided custom slug is already in use. Please choose a different value.",
                req,
                "errorCode", "SLUG_ALREADY_EXISTS"
        );
    }

    @ExceptionHandler(InvalidTargetUrlException.class)
    public ProblemDetail handleInvalidUrl(InvalidTargetUrlException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid target URL", ex.getMessage(), req,
                "errorCode", "INVALID_TARGET_URL");
    }

    // ======================================================================
    // Validation / request errors (400)
    // ======================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation error",
                "Request validation failed", req,
                "errorCode", "VALIDATION_ERROR");

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String detail = "Invalid value for parameter '%s'".formatted(ex.getName());
        return problem(HttpStatus.BAD_REQUEST, "Bad request", detail, req,
                "errorCode", "TYPE_MISMATCH");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "Malformed JSON request", req,
                "errorCode", "MALFORMED_JSON");
    }

    // ======================================================================
    // ResponseStatusException (404/410/etc from service layer)
    // ======================================================================

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        String title = status.getReasonPhrase();
        String detail = ex.getReason() != null ? ex.getReason() : title;

        return problem(status, title, detail, req,
                "errorCode", "HTTP_" + status.value());
    }

    // ======================================================================
    // Auth (обычно только для твоего /auth/login, НЕ для JWT фильтра)
    // ======================================================================

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password", req,
                "errorCode", "UNAUTHORIZED");
    }

    // ======================================================================
    // Fallback 500
    // ======================================================================

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAny(Exception ex, HttpServletRequest req) {
        // ex логируй отдельно (logger.error), но не отдавай stack trace клиенту
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error", req,
                "errorCode", "INTERNAL_ERROR");
    }

    // ======================================================================
    // Helper
    // ======================================================================

    private ProblemDetail problem(HttpStatus status,
                                  String title,
                                  String detail,
                                  HttpServletRequest req,
                                  Object... propsKv) {

        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);

        // полезно для дебага на клиенте
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());

        // кастомные свойства парами key,value
        for (int i = 0; i + 1 < propsKv.length; i += 2) {
            pd.setProperty(String.valueOf(propsKv[i]), propsKv[i + 1]);
        }

        return pd;
    }
}
