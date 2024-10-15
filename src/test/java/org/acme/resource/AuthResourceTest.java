package org.acme.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import org.acme.exceptions.RegistrationFailedException;
import org.acme.model.Auth;
import org.acme.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class AuthResourceTest {

    @InjectMock
    private AuthService authService;
    private Auth authResponse;

    @BeforeEach
    void setUp() {
        authResponse = new Auth();
        authResponse.setEmail("mock@gmail.com");
        authResponse.setPassword("m)ck!123POwed");
        authResponse.setFirstName("mock first name");
        authResponse.setLastName("mcok last name");
    }


    @Test
    void givenValidAuthRequest_whenRegisterUserCalled_thenReturn201() {
        Mockito.when(authService.createUser(any(Auth.class)))
                .thenReturn(Uni.createFrom().item(authResponse));
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(authResponse)
                .when().post("/auth/register")
                .then()
                .statusCode(201);
    }

    @Test
    void givenInvalidAuthRequest_whenRegisterUserCalled_thenReturn500() {
        Mockito.when(authService.createUser(any(Auth.class)))
                .thenReturn(Uni.createFrom().failure(new RegistrationFailedException("MOCK")));
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(authResponse)
                .when().post("/auth/register")
                .then()
                .statusCode(500);
    }
}