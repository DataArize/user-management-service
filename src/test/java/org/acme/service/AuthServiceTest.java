package org.acme.service;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.acme.entity.User;
import org.acme.exceptions.RegistrationFailedException;
import org.acme.model.Auth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@QuarkusTest
class AuthServiceTest {

    private Auth authRequest;
    @Inject
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authRequest = new Auth();
        authRequest.setEmail("mock@gmail.com");
        authRequest.setPassword("m)ck!123POwed");
        authRequest.setFirstName("mock first name");
        authRequest.setLastName("mcok last name");
        PanacheMock.mock(User.class);
    }

    @Test
    void givenValidAuthRequest_whenCreateUserCalled_thenReturnAuthResponse() {
        Mockito.when(User.persist(any(User.class), any(Object.class))).thenReturn(Uni.createFrom().voidItem());
        Auth authResponse = authService.createUser(authRequest).await().indefinitely();
        assertEquals(authRequest.getEmail(), authResponse.getEmail());
    }

    @Test
    void givenValidAuthRequest_whenCreateUserCalled_thenThrowsException() {
        Mockito.when(User.persist(any(User.class), any(Object.class))).thenReturn(Uni.createFrom().failure(new IOException("Mock exception")));
        assertThrows(CompositeException.class, () -> authService.createUser(authRequest).await().indefinitely());
    }

}