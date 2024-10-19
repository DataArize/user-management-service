package org.acme.service;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.constants.Query;
import org.acme.entity.Token;
import org.acme.entity.User;
import org.acme.exceptions.InvalidRefreshTokenException;
import org.acme.exceptions.RefreshTokenExpiredException;
import org.acme.exceptions.UnableToPersistException;
import org.acme.model.AuthToken;
import org.acme.utils.JwtUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.function.Function;

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

    /**
     * Generates authentication tokens for the specified user.
     *
     * This method creates both an access token and a refresh token for the
     * provided user. The tokens are generated with a specified expiration time.
     *
     * @param user the user for whom the tokens will be generated.
     * @return a {@link Uni} containing an {@link AuthToken} with the generated
     *         access and refresh tokens.
     */
    public Uni<AuthToken> generateTokens(User user) {
        String accessToken = JwtUtils.generateAccessToken(user, expirationDays);
        String refreshToken = JwtUtils.generateRefreshToken(user, expirationDays * 24 * 60 * 60);
        return Uni.createFrom().item(() -> new AuthToken(accessToken, refreshToken, expirationDays));
    }

    /**
     * Generates and persists authentication tokens for the specified user.
     *
     * This method creates new authentication tokens for the given user and
     * stores the refresh token in the database along with its expiration time.
     *
     * @param user the user for whom the tokens will be generated and persisted.
     * @return a {@link Uni} containing an {@link AuthToken} with the generated
     *         access and refresh tokens.
     */
    public Uni<AuthToken> generateAndPersistTokens(User user) {
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expirationDays * 24 * 60 * 60);
        return generateTokens(user)
                .onItem().transformToUni(tokens -> persistToken(user.id, tokens.getRefreshToken(), expirationTime)
                        .onItem().transform(success -> tokens));
    }

    /**
     * Persists the refresh token and its expiration time for the specified user.
     *
     * This method creates a new token entry in the database with the user ID,
     * refresh token, and its expiration time. If the persistence operation fails,
     * an exception is thrown.
     *
     * @param userId the ID of the user to whom the token belongs.
     * @param refreshToken the refresh token to be persisted.
     * @param expiration the expiration time of the refresh token.
     * @return a {@link Uni<Void>} indicating the completion of the persistence operation.
     * @throws UnableToPersistException if there is an error during the persistence operation.
     */
    public Uni<Void> persistToken(Long userId, String refreshToken, LocalDateTime expiration) {
        Token token = new Token(userId, refreshToken, expiration);
        return Panache.withTransaction(() -> PanacheEntityBase.persist(token))
                .onItem().transform(success -> {
                    log.info("Persisted refresh token details for userId: {}", userId);
                    return Uni.createFrom().voidItem();
                }).onFailure().transform(ex -> {
                    log.error("Unable to persist refresh token for userId: {}", userId);
                    throw new UnableToPersistException(ExceptionMessages.UNABLE_TO_PERSIST_TOKEN_DETAILS + ex.getMessage(), ErrorCodes.PERSISTENCE_FAILED);
                }).replaceWithVoid();
    }

    /**
     * Generates a new access token based on the provided refresh token.
     *
     * This method parses the refresh token to extract the user ID and validates the refresh token.
     * If valid, it retrieves the user by ID and generates a new access token for the user.
     *
     * @param authToken the {@link AuthToken} containing the refresh token.
     * @param findByUserId a function that retrieves a user by their ID.
     * @return a {@link Uni<AuthToken>} containing the newly generated access token.
     * @throws InvalidRefreshTokenException if the refresh token is invalid or cannot be validated.
     * @throws RefreshTokenExpiredException if the refresh token has expired.
     */
    public Uni<AuthToken> generateAccessToken(AuthToken authToken, Function<Long, Uni<User>> findByUserId) {
        return parseRefreshToken(authToken.getRefreshToken())
                    .onItem().transformToUni(userId -> validateRefreshToken(userId, authToken.getRefreshToken())
                        .onItem().transformToUni(valid -> {
                            if (Boolean.FALSE.equals(valid)) {
                                throw new InvalidRefreshTokenException(ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.INVALID_TOKEN);
                            }
                            return findByUserId.apply(userId);
                        })
                ).onItem().transformToUni(this::generateAndPersistTokens)
                .onFailure().transform(
                        ex -> {
                            log.error("Error generating new access token: Exception: {}", ex.getMessage());
                            throw new InvalidRefreshTokenException(ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.INVALID_TOKEN);
                });
    }

    /**
     * Parses the given refresh token to extract the user ID.
     *
     * This method attempts to parse the refresh token and retrieves the user ID
     * from the token's subject. If parsing fails, it throws an exception.
     *
     * @param refreshToken the refresh token to parse.
     * @return a {@link Uni<Long>} containing the user ID extracted from the refresh token.
     * @throws InvalidRefreshTokenException if the refresh token cannot be parsed.
     */
    public Uni<Long> parseRefreshToken(String refreshToken) {
        try {
            JsonWebToken jsonWebToken = jwtParser.parse(refreshToken);
            return Uni.createFrom().item(Long.parseLong(jsonWebToken.getSubject()));
        } catch (ParseException ex) {
            log.error("Failed to parse refresh token: {}", ex.getMessage());
            return Uni.createFrom().failure(new InvalidRefreshTokenException(ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.INVALID_TOKEN));
        }
    }

    /**
     * Validates the provided refresh token against the stored token for the user.
     *
     * This method checks if the refresh token matches the stored token and whether it has expired.
     *
     * @param userId the ID of the user whose refresh token is being validated.
     * @param refreshToken the refresh token to validate.
     * @return a {@link Uni<Boolean>} indicating whether the refresh token is valid.
     * @throws InvalidRefreshTokenException if the refresh token does not match or cannot be found.
     * @throws RefreshTokenExpiredException if the refresh token has expired.
     */
    public Uni<Boolean> validateRefreshToken(Long userId, String refreshToken) {
        return Panache.withTransaction(() -> Token.find(Query.FETCH_REFRESH_TOKEN, userId).firstResult())
                .onItem().transform(entity -> {
                    Token token = (Token) entity;
                    if (!token.getRefreshToken().equals(refreshToken)) {
                        log.warn("Refresh token mismatch for userId: {}", userId);
                        throw new InvalidRefreshTokenException(ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.INVALID_TOKEN);
                    }
                    if (JwtUtils.validateExpiration(token.getExpiresAt())) {
                        log.warn("Refresh token expired for userId: {}", userId);
                        throw new RefreshTokenExpiredException(ExceptionMessages.REFRESH_TOKEN_EXPIRED, ErrorCodes.TOKEN_EXPIRED);
                    }
                    return true;
                })
                .onFailure().transform(ex -> {
                    log.error("Unable to find token for user with id: {}", userId);
                    throw new InvalidRefreshTokenException(ExceptionMessages.INVALID_REFRESH_TOKEN, ErrorCodes.INVALID_TOKEN);
                });
    }


}
