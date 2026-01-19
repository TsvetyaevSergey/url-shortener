package dev.horoz.url_shortener.dto.auth;

public record LoginResponseDto(
        String email,
        String accessToken
) {}
