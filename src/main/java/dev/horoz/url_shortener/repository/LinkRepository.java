package dev.horoz.url_shortener.repository;

import dev.horoz.url_shortener.domain.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {
    Optional<Link> findByTargetUrlIgnoreCase(String targetUrl);
}
