package dev.horoz.url_shortener.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue
    private UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @Column(nullable = false, length = 32, unique = true)
    private String slug;

    @Setter
    @Column(name = "target_url", nullable = false, columnDefinition = "TEXT")
    private String targetUrl;
    @Setter
    @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
    private Instant expiresAt;

    @Column(name = "clicks_total", insertable = false)
    private Long clicksTotal;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Link that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
