package dev.horoz.url_shortener.dto.link;

import java.time.Instant;
import java.util.UUID;

public record LinkResponseDto(
        UUID id,
        String slug,
        String targetUrl,
        Instant expiresAt,
        Long clicksTotal,
        Instant createdAt
) {
}
