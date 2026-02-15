package dev.horoz.url_shortener.controller;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.dto.link.LinkCreateRequestDto;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;
import dev.horoz.url_shortener.dto.link.LinkStatsDto;
import dev.horoz.url_shortener.mapper.LinkMapper;
import dev.horoz.url_shortener.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }


    @PostMapping("/create")
    public LinkResponseDto create(@Valid Authentication authentication, @RequestBody LinkCreateRequestDto dto) {
        return linkService.createLink(authentication, dto.targetUrl(), dto.customSlug());
    }

    @GetMapping
    public Page<LinkResponseDto> getLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return linkService.getLinks(authentication, page, size);
    }

    @GetMapping("/{id}")
    public LinkResponseDto getById(@PathVariable UUID id, Authentication authentication) {
        return linkService.getLinkById(authentication, id);
    }
    @GetMapping("/{id}/stats")
    public LinkStatsDto getLinkStatsById(@PathVariable UUID id, Authentication authentication) {
        return linkService.getLinkStatsById(authentication, id);
    }

    @PatchMapping("/{id}")
    public LinkResponseDto updateLink(@PathVariable UUID id, Authentication authentication) {
        return linkService.updateLink(authentication, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        linkService.deleteLinkById(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
