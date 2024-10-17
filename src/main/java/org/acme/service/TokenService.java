package org.acme.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.Errors;
import org.acme.constants.Query;
import org.acme.entity.Token;
import org.acme.entity.User;
import org.acme.exceptions.InvalidRefreshTokenException;
import org.acme.exceptions.RefreshTokenExpiredException;
import org.acme.model.AuthToken;
import org.acme.utils.JwtUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Slf4j
@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "jwt.expiration")
    private Long expirationDays;
    private final JWTParser jwtParser;

    @Inject
    public TokenService(JWTParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    public Uni<AuthToken> generateNewAccessToken(AuthToken authToken) {
        return Uni.createFrom().item(authToken)
                .onItem().transformToUni(token -> parseRefreshToken(authToken.getRefreshToken())
                        .onItem().transformToUni(userId -> validateRefreshToken(userId, authToken.getRefreshToken())
                                .onItem().transformToUni(valid -> {
                                    if (!valid) {
                                        throw new InvalidRefreshTokenException(Errors.INVALID_REFRESH_TOKEN);
                                    }
                                    return findByUserId(userId);
                                })
                        )
                )
                .onItem().transformToUni(this::createTokensForUser)
                .onFailure().transform(
                        ex -> {
                            log.error("Error generating new access token: {}", ex.getMessage());
                            throw new InvalidRefreshTokenException(Errors.INVALID_REFRESH_TOKEN);
                });
    }

    private Uni<Long> parseRefreshToken(String refreshToken) {
        try {
            JsonWebToken jsonWebToken = jwtParser.parse(refreshToken);
            return Uni.createFrom().item(Long.parseLong(jsonWebToken.getSubject()));
        } catch (ParseException ex) {
            log.error("Failed to parse refresh token: {}", ex.getMessage());
            return Uni.createFrom().failure(new InvalidRefreshTokenException(Errors.INVALID_REFRESH_TOKEN));
        }
    }

    public Uni<User> findByUserId(Long userId) {
        return Panache.withTransaction(() -> User.findById(userId))
                .onItem().transform(User.class::cast)
                .onFailure().invoke(ex -> log.error("Invalid Refresh token, user Id : {} not found", userId))
                .onFailure().transform(ex -> new InvalidRefreshTokenException(Errors.INVALID_REFRESH_TOKEN));
    }

    public Uni<Boolean> validateRefreshToken(Long userId, String refreshToken) {
        return Panache.withTransaction(() -> Token.find(Query.FETCH_REFRESH_TOKEN, userId).firstResult())
                .onItem().transform(entity -> {
                    Token token = (Token) entity;
                    if (!token.getRefreshToken().equals(refreshToken)) {
                        log.warn("Refresh token mismatch for userId: {}", userId);
                        throw new InvalidRefreshTokenException(Errors.INVALID_REFRESH_TOKEN);
                    }
                    if (JwtUtils.validateExpiration(token.getExpiresAt())) {
                        log.warn("Refresh token expired for userId: {}", userId);
                        throw new RefreshTokenExpiredException(Errors.REFRESH_TOKEN_EXPIRED);
                    }
                    return true;
                })
                .onFailure().recoverWithItem(ex -> {
                    log.error("Unable to find token for user with id: {}", userId);
                    return false;
                });
    }

    private Uni<AuthToken> createTokensForUser(User user) {
        return generateTokens(user)
                .onItem().transform(tokens -> {
                    log.info("Generated new tokens for userId: {}", user.id);
                    return tokens;
                });
    }

    private Uni<AuthToken> generateTokens(User user) {
        String accessToken = JwtUtils.generateAccessToken(user, expirationDays);
        String refreshToken = JwtUtils.generateRefreshToken(user, expirationDays * 24 * 60 * 60);
        return Uni.createFrom().item(() -> new AuthToken(accessToken, refreshToken, expirationDays));
    }
}
