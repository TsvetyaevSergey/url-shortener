package dev.horoz.url_shortener.controller;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.dto.link.LinkCreateRequestDto;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;
import dev.horoz.url_shortener.mapper.LinkMapper;
import dev.horoz.url_shortener.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }


    @PostMapping("/create")
    public LinkResponseDto create(@Valid Authentication authentication, @RequestBody LinkCreateRequestDto dto) {
        Link link = linkService.createLink(authentication, dto.targetUrl(), dto.expiresAt());
        return LinkMapper.toDto(link);
    }

}
