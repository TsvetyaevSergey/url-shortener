package dev.horoz.url_shortener.repository;

import dev.horoz.url_shortener.domain.Link;
import dev.horoz.url_shortener.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {
    Optional<Link> findByTargetUrlIgnoreCase(String targetUrl);
    Optional<Link> findByUserAndTargetUrlIgnoreCase(User user, String targetUrl);
    Page<Link> findAllByUser(User user, Pageable pageable);
    Optional<Link> findByIdAndUser(UUID id, User user);
    Optional<Link> findBySlug(String slug);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Link l set l.clicksTotal = l.clicksTotal + 1 where l.id = :id")
    void incrementClicksTotal(@Param("id") UUID id);
}
