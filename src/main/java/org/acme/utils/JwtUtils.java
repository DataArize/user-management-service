package org.acme.utils;

import io.smallrye.jwt.build.Jwt;
import lombok.experimental.UtilityClass;
import org.acme.entity.Role;
import org.acme.entity.User;

import java.time.Instant;
import java.util.stream.Collectors;

@UtilityClass
public class JwtUtils {

    public static String generateAccessToken(User user, long expiration) {
        return Jwt.issuer("https://houseofllm.com")
                .subject(String.valueOf(user.id))
                .upn(user.getEmail())
                .groups(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(expiration))
                .sign();
    }

    public static String generateRefreshToken(User user, long expiration) {
        return Jwt.issuer("https://houseofllm.com")
                .subject(String.valueOf(user.id))
                .groups(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(expiration))
                .sign();
    }
}
