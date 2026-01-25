package dev.horoz.url_shortener.service.validation;
import dev.horoz.url_shortener.exceptions.InvalidTargetUrlException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Service
public class UrlValidationService {

    public String normalizeAndValidateUrl(String rawUrl) {
        if (rawUrl.isBlank()) {
            throw new InvalidTargetUrlException("targetUrl is required");
        }

        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidTargetUrlException("targetUrl must not be blank");
        }

        final URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new InvalidTargetUrlException("targetUrl must be a valid URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new InvalidTargetUrlException("targetUrl must include a scheme (http or https)");
        }

        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidTargetUrlException("Only http and https URLs are allowed");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidTargetUrlException("targetUrl must include a valid host");
        }

        return trimmed;
    }

}
