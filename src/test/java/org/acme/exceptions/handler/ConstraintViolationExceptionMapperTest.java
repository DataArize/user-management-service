package org.acme.exceptions.handler;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import org.acme.model.ErrorResponse;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ConstraintViolationExceptionMapperTest {

    private ConstraintViolationExceptionMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new ConstraintViolationExceptionMapper();
    }

    @Test
    void givenValidConstraintViolationException_whenToResponseCalled_thenReturnBadRequest() {
        ConstraintViolation<?> violation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(violation.getPropertyPath()).thenReturn(Mockito.mock(Path.class));
        Mockito.when(violation.getMessage()).thenReturn("Password is too short.");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(violations);
        HibernateException hibernateException = new HibernateException("Some error", constraintViolationException);
        Response response = mapper.toResponse(hibernateException);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
    }

    @Test
    void givenNonConstraintViolationException_whenToResponseCalled_thenReturnInternalServerError() {
        HibernateException hibernateException = new HibernateException("Some other error");
        Response response = mapper.toResponse(hibernateException);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        ErrorResponse errorResponse = (ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
    }

}