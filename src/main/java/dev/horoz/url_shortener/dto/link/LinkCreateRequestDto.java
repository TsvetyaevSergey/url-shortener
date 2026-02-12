package dev.horoz.url_shortener.dto.link;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LinkCreateRequestDto(

        @NotBlank
        String targetUrl,

        @Size(min = 3, max = 32)
        @Nullable
        String customSlug

) {
}
