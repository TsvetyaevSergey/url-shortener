package dev.horoz.url_shortener.repository;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {
    Optional<Link> findByTargetUrlIgnoreCase(String targetUrl);
    Page<Link> findAllByUser(User user, Pageable pageable);
    Optional<Link> findByIdAndUser(UUID id, User user);

}
