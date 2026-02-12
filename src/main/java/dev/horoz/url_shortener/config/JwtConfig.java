package dev.horoz.url_shortener.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(@Value("${app.security.jwt.secret}") String secret) {
        // HS256
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Мы будем класть роли в claim "roles": ["USER", ...]
        // и преобразовывать их в GrantedAuthority вида ROLE_USER
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object rolesObj = jwt.getClaims().get("roles");
            if (rolesObj instanceof List<?> roles) {
                return roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();
            }
            return List.of();
        });
        return converter;
    }
}
