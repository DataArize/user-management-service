package org.acme.utils;

import io.smallrye.jwt.build.Jwt;
import lombok.experimental.UtilityClass;
import org.acme.constants.JwtToken;
import org.acme.entity.Role;
import org.acme.entity.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@UtilityClass
public class JwtUtils {

    public static String generateAccessToken(User user, long expiration) {
        return Jwt.issuer(JwtToken.ISSUER)
                .subject(String.valueOf(user.id))
                .claim(JwtToken.CLAIM_TYPE, JwtToken.ACCESS_TOKEN)
                .groups(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .issuedAt(Instant.now())
                .audience(JwtToken.AUDIENCE)
                .expiresAt(Instant.now().plusSeconds(expiration))
                .sign();
    }

    public static String generateRefreshToken(User user, long expiration) {
        return Jwt.issuer(JwtToken.ISSUER)
                .subject(String.valueOf(user.id))
                .groups(user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .claim(JwtToken.CLAIM_TYPE, JwtToken.REFRESH_TOKEN)
                .issuedAt(Instant.now())
                .audience(JwtToken.AUDIENCE)
                .expiresAt(Instant.now().plusSeconds(expiration))
                .sign();
    }

    public static boolean validateIssuer(String issuer) {
        return issuer.equalsIgnoreCase(JwtToken.ISSUER);
    }

    public static boolean validateExpiration(LocalDateTime expiration) {
        return !expiration.isAfter(LocalDateTime.now());
    }
}
