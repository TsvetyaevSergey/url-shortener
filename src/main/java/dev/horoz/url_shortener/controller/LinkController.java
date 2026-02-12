package dev.horoz.url_shortener.controller;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.dto.link.LinkCreateRequestDto;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;
import dev.horoz.url_shortener.mapper.LinkMapper;
import dev.horoz.url_shortener.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }


    @PostMapping("/create")
    public LinkResponseDto create(@Valid Authentication authentication, @RequestBody LinkCreateRequestDto dto) {
        Link link = linkService.createLink(authentication, dto.targetUrl());
        return LinkMapper.toDto(link);
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
        return linkService.getLink(authentication, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        linkService.deleteLink(authentication, id);
        return ResponseEntity.noContent().build();
    }



}
