package dev.horoz.url_shortener.api.auth.dto;

public record LoginResponse(
        String email,
        String accessToken
) {}
