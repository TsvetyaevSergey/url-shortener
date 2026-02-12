package dev.horoz.url_shortener.service;

import dev.horoz.url_shortener.exceptions.EmailAlreadyExistsException;
import dev.horoz.url_shortener.domain.User;
import dev.horoz.url_shortener.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    private final long ttlSeconds;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.ttl:3600}") long ttlSeconds
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public void register(String email, String rawPassword) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    public String loginAndIssueToken(String email, String rawPassword) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication authentication = authenticationManager.authenticate(authRequest);

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("url-shortener")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(authentication.getName())
                .build();

        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
