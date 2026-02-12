package dev.horoz.url_shortener.controller.advice;

import dev.horoz.url_shortener.exceptions.EmailAlreadyExistsException;
import dev.horoz.url_shortener.exceptions.InvalidTargetUrlException;
import dev.horoz.url_shortener.exceptions.SlugAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Email already exists");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(InvalidTargetUrlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail invalidUrl(InvalidTargetUrlException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid target URL");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation error");
        pd.setDetail("Request validation failed");
        return pd;
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail conflictSlug(SlugAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Slug already exists");
        pd.setDetail("The provided custom slug is already in use. Please choose a different value.");
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuth(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setDetail("Invalid email or password");
        return pd;
    }

}
