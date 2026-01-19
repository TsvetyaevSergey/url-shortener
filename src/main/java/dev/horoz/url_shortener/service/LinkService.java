package dev.horoz.url_shortener.service;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import dev.horoz.url_shortener.repository.LinkRepository;
import dev.horoz.url_shortener.repository.UserRepository;
import dev.horoz.url_shortener.service.slug.SlugGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LinkService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final SlugGenerator slugGenerator;

    public LinkService(UserRepository userRepository,
                       LinkRepository linkRepository,
                       SlugGenerator slugGenerator) {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public Link createLink(Authentication authentication, String targetUrl, Instant expiresAt) {
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        Link link = new Link();
        link.setUser(user);
        link.setTargetUrl(targetUrl);
        link.setSlug(slugGenerator.nextSlug());
        link.setExpiresAt(expiresAt);

        return linkRepository.save(link);
    }
}
