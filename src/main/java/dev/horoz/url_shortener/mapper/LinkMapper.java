package dev.horoz.url_shortener.mapper;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;

public final class LinkMapper {

    private LinkMapper() {}

    public static LinkResponseDto toDto(Link link) {
        return new LinkResponseDto(
                link.getId(),
                link.getSlug(),
                link.getTargetUrl(),
                link.getExpiresAt(),
                link.getClicksTotal(),
                link.getCreatedAt()
        );
    }
}
