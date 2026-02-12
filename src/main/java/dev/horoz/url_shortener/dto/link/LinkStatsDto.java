package dev.horoz.url_shortener.dto.link;

import java.time.Instant;

public record LinkStatsDto(
        Long clicksTotal,
        Instant createdAt,
        Instant expiresAt
) {
}
