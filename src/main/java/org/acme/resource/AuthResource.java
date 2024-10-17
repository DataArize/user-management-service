package org.acme.resource;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.acme.constants.AccountRole;
import org.acme.constants.Errors;
import org.acme.model.Auth;
import org.acme.model.AuthToken;
import org.acme.model.ErrorResponse;
import org.acme.model.User;
import org.acme.service.AuthService;
import org.acme.service.TokenService;

import java.util.Objects;

@Slf4j
@Path("/auth")
public class AuthResource {

    private final AuthService authService;
    private final TokenService tokenService;

    @Inject
    public AuthResource(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> registerUser(@Valid Auth authRequest) {
        return authService.createUser(authRequest)
                .onItem().transform(success -> Response.status(Response.Status.CREATED)
                        .entity(success).build())
                .onFailure()
                .recoverWithItem(ex -> {
                    log.error("Unable to create User, email: {}, Exception: {}", authRequest.getEmail(), ex.getMessage(), ex);
                    ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
                });
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> loginUser(@Valid Auth authRequest) {
        return authService.authenticateUser(authRequest)
                .onItem().transform(success -> Response.status(Response.Status.OK).entity(success).build())
                .onFailure()
                .recoverWithItem(ex -> {
                    log.error("Invalid login credentials, email: {}, Exception: {}", authRequest.getEmail(), ex.getMessage(), ex);
                    ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                });
    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> refreshToken(AuthToken authToken) {
        return tokenService.generateNewAccessToken(authToken)
                .onItem().transform(success -> Response.status(Response.Status.OK).entity(success).build())
                .onFailure()
                .recoverWithItem(ex -> {
                    log.error("Invalid refresh token, Exception: {}", ex.getMessage(), ex);
                    ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                });

    }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getCurrentUser(@Context SecurityContext securityContext) {
        return authService.findByUserId(Long.parseLong(securityContext.getUserPrincipal().getName()))
                .onItem().transform(user -> {
                    if(Objects.isNull(user))  {
                        log.error("User is not authenticated");
                        ErrorResponse errorResponse = new ErrorResponse(Errors.USER_IS_NOT_AUTHENTICATED, Response.Status.BAD_REQUEST);
                        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                    }
                    User userResponse = new User(user.getEmail(), user.getFirstName(),
                            user.getLastName(), user.getRoles(), user.getStatus(),
                            user.getQuota(), user.getLastLogin());
                    return Response.status(Response.Status.OK).entity(userResponse).build();
                })
                .onFailure()
                .recoverWithItem(ex -> {
                    log.error("Invalid Auth token, Exception: {}", ex.getMessage(), ex);
                    ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), Response.Status.BAD_REQUEST);
                    return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                });
    }



}
