package dev.horoz.url_shortener.service.validation;

import dev.horoz.url_shortener.exceptions.InvalidTargetUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UrlValidationServiceTest {

    private UrlValidationService service;

    @BeforeEach
    void setUp() {
        service = new UrlValidationService();
    }

    @Test
    @DisplayName("rawUrl isBlank -> throws: targetUrl is required")
    void shouldThrowWhenRawUrlIsBlank() {
        assertThatThrownBy(() -> service.normalizeAndValidateUrl(""))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl is required");

        assertThatThrownBy(() -> service.normalizeAndValidateUrl("   "))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl is required");

        assertThatThrownBy(() -> service.normalizeAndValidateUrl("\n\t"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl is required");
    }

    @Test
    @DisplayName("trimmed isEmpty (после trim) -> throws: targetUrl must not be blank")
    void shouldThrowWhenTrimmedIsEmpty() {
        String raw = "   ";
        assertThatThrownBy(() -> service.normalizeAndValidateUrl(raw))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl is required");
    }

    @Test
    @DisplayName("Invalid URI syntax -> throws: targetUrl must be a valid URL")
    void shouldThrowWhenUriSyntaxIsInvalid() {
        assertThatThrownBy(() -> service.normalizeAndValidateUrl("http://exa mple.com"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must be a valid URL");

        assertThatThrownBy(() -> service.normalizeAndValidateUrl("http://"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must be a valid URL");
    }

    @Test
    @DisplayName("Missing scheme -> throws: targetUrl must include a scheme (http or https)")
    void shouldThrowWhenSchemeMissing() {
        assertThatThrownBy(() -> service.normalizeAndValidateUrl("example.com/path"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must include a scheme (http or https)");

        assertThatThrownBy(() -> service.normalizeAndValidateUrl("//example.com/path"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must include a scheme (http or https)");
    }

    @Test
    @DisplayName("Unsupported scheme -> throws: Only http and https URLs are allowed")
    void shouldThrowWhenSchemeNotHttpOrHttps() {
        assertThatThrownBy(() -> service.normalizeAndValidateUrl("ftp://example.com"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("Only http and https URLs are allowed");

        assertThatThrownBy(() -> service.normalizeAndValidateUrl("mailto:test@example.com"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("Only http and https URLs are allowed");
    }

    @Test
    @DisplayName("Scheme is case-insensitive: HTTP/HTTPS are accepted")
    void shouldAcceptUppercaseScheme() {
        String result1 = service.normalizeAndValidateUrl("HTTP://example.com");
        String result2 = service.normalizeAndValidateUrl("Https://example.com/path");

        assertThat(result1).isEqualTo("HTTP://example.com");
        assertThat(result2).isEqualTo("Https://example.com/path");
    }

    @Test
    @DisplayName("Missing host -> throws: targetUrl must include a valid host")
    void shouldThrowWhenHostMissing() {
        // Для URI вида http:/path host == null
        assertThatThrownBy(() -> service.normalizeAndValidateUrl("http:/path"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must include a valid host");

        // Для localhost в абсолютном URI host будет "localhost"
        assertThatThrownBy(() -> service.normalizeAndValidateUrl("https:///just-path"))
                .isInstanceOf(InvalidTargetUrlException.class)
                .hasMessage("targetUrl must include a valid host");
    }

    @Test
    @DisplayName("Happy path: returns trimmed URL")
    void shouldReturnTrimmedUrlWhenValid() {
        String raw = "   https://example.com/path?q=1   ";
        String result = service.normalizeAndValidateUrl(raw);

        assertThat(result).isEqualTo("https://example.com/path?q=1");
    }

    @Test
    @DisplayName("Accepts localhost and ports (valid host)")
    void shouldAcceptLocalhostAndPort() {
        assertThat(service.normalizeAndValidateUrl("http://localhost:8080/api"))
                .isEqualTo("http://localhost:8080/api");

        assertThat(service.normalizeAndValidateUrl("https://127.0.0.1:8443"))
                .isEqualTo("https://127.0.0.1:8443");
    }
}
