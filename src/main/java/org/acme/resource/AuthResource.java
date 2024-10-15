package org.acme.resource;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.acme.model.Auth;
import org.acme.model.ErrorResponse;
import org.acme.service.AuthService;

@Slf4j
@Path("/auth")
public class AuthResource {

    private final AuthService authService;

    @Inject
    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/register")
    @WithTransaction
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


}
