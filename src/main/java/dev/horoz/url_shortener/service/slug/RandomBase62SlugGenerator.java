package dev.horoz.url_shortener.service.slug;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomBase62SlugGenerator implements SlugGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextSlug() {
        int length = 8;
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(buf);
    }
}
