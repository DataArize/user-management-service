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

    /**
     * Generates an access token for a specified user with a given expiration time.
     *
     * This method creates a JWT access token that includes the user ID, roles,
     * and other claims such as the token type and audience. The token is set to
     * expire after the specified duration.
     *
     * @param user        the {@link User} for whom the access token is generated.
     * @param expiration  the token's expiration time in seconds.
     * @return            a signed JWT access token as a {@link String}.
     */
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

    /**
     * Generates a refresh token for a specified user with a given expiration time.
     *
     * This method creates a JWT refresh token that includes the user ID, roles,
     * and other claims such as the token type and audience. The token is set to
     * expire after the specified duration.
     *
     * @param user        the {@link User} for whom the refresh token is generated.
     * @param expiration  the token's expiration time in seconds.
     * @return            a signed JWT refresh token as a {@link String}.
     */
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

    /**
     * Generates a password reset token for a specified user ID with a given expiration time.
     *
     * This method creates a JWT password reset token that includes the user ID
     * and other claims such as the token type and audience. The token is set to
     * expire after the specified duration.
     *
     * @param userId      the ID of the user for whom the password reset token is generated.
     * @param expiration  the token's expiration time in seconds.
     * @return            a signed JWT password reset token as a {@link String}.
     */
    public static String generatePasswordResetToken(Long userId, long expiration) {
        return Jwt.issuer(JwtToken.ISSUER)
                .subject(String.valueOf(userId))
                .claim(JwtToken.CLAIM_TYPE, JwtToken.PASSWORD_RESET_TOKEN)
                .issuedAt(Instant.now())
                .audience(JwtToken.AUDIENCE)
                .expiresAt(Instant.now().plusSeconds(expiration))
                .sign();
    }

    /**
     * Validates whether a given expiration time has already passed.
     *
     * This method checks if the provided {@link LocalDateTime} expiration is
     * before or equal to the current time, indicating that the token has expired.
     *
     * @param expiration  the expiration time to validate.
     * @return            {@code true} if the expiration time is in the past or present;
     *                    {@code false} otherwise.
     */
    public static boolean validateExpiration(LocalDateTime expiration) {
        return !expiration.isAfter(LocalDateTime.now());
    }

    public static boolean compareToken(String expectedToken, String actualToken) {
        return expectedToken.equals(actualToken);
    }
}
