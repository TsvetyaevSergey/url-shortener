package dev.horoz.url_shortener.repository;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {
}
