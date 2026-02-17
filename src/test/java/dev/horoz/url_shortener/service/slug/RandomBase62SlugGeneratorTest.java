package dev.horoz.url_shortener.service.slug;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class RandomBase62SlugGeneratorTest {

    private RandomBase62SlugGenerator generator;

    private static final String BASE62_REGEX = "^[0-9A-Za-z]{8}$";

    @BeforeEach
    void setUp() {
        generator = new RandomBase62SlugGenerator();
    }

    @Test
    void shouldGenerateSlugWithLength8() {
        String slug = generator.nextSlug();

        assertThat(slug)
                .isNotNull()
                .hasSize(8);
    }

    @Test
    void shouldGenerateOnlyBase62Characters() {
        String slug = generator.nextSlug();

        assertThat(slug).matches(BASE62_REGEX);
    }

    @RepeatedTest(20)
    void shouldAlwaysGenerateValidSlug() {
        String slug = generator.nextSlug();

        assertThat(slug)
                .isNotNull()
                .matches(BASE62_REGEX);
    }

    @Test
    void shouldGenerateDifferentSlugs() {
        Set<String> slugs = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            slugs.add(generator.nextSlug());
        }

        assertThat(slugs.size()).isGreaterThan(95);
    }
}
