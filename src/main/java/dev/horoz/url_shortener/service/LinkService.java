package dev.horoz.url_shortener.service;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import dev.horoz.url_shortener.dto.link.LinkResponseDto;
import dev.horoz.url_shortener.mapper.LinkMapper;
import dev.horoz.url_shortener.repository.LinkRepository;
import dev.horoz.url_shortener.repository.UserRepository;
import dev.horoz.url_shortener.service.slug.SlugGenerator;
import dev.horoz.url_shortener.service.validation.UrlValidationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class LinkService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final SlugGenerator slugGenerator;
    private final UrlValidationService urlValidationService;

    public LinkService(UserRepository userRepository,
                       LinkRepository linkRepository,
                       SlugGenerator slugGenerator,
                       UrlValidationService urlValidationService) {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.slugGenerator = slugGenerator;
        this.urlValidationService = urlValidationService;
    }

    @Transactional
    public Link createLink(Authentication authentication, String targetUrl, Instant expiresAt) {
        String email = authentication.getName();
        String validUrl = urlValidationService.normalizeAndValidateUrl(targetUrl);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        Optional<Link> existingLink = linkRepository.findByTargetUrlIgnoreCase(validUrl);
        if (existingLink.isPresent()) return existingLink.get();

        Link link = new Link();
        link.setUser(user);
        link.setTargetUrl(validUrl);
        link.setExpiresAt(expiresAt);
        int MAX_ATTEMPTS = 10;
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String slug = slugGenerator.nextSlug();
            link.setSlug(slug);
            try {
                linkRepository.saveAndFlush(link);
                return link;
            } catch (DataIntegrityViolationException e) {
                if (isUniqueSlugViolation(e)) {
                    continue;
                }
                throw e;
            }

        }
        throw new IllegalStateException(
                String.format("Could not generate unique slug after %d attempts", MAX_ATTEMPTS)
        );
    }

    public Page<LinkResponseDto> getLinks(Authentication authentication, Integer page, Integer size) {
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()


        );

        return linkRepository.findAllByUser(user,pageable).map(LinkMapper::toDto);
    }

    private boolean isUniqueSlugViolation(DataIntegrityViolationException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                String name = cve.getConstraintName();
                return name != null && name.equalsIgnoreCase("links_slug_uidx");
            }
            t = t.getCause();
        }
        return false;
    }


}
