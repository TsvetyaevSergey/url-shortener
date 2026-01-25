package dev.horoz.url_shortener.exceptions;

public class InvalidTargetUrlException extends RuntimeException {
    public InvalidTargetUrlException(String message) {
        super(message);
    }
}
