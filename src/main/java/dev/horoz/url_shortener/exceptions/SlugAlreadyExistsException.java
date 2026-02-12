package dev.horoz.url_shortener.exceptions;

public class SlugAlreadyExistsException extends RuntimeException {
    public SlugAlreadyExistsException(String slug) {
        super("Slug already exist: " + slug);
    }
}
