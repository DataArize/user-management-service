package org.acme.exceptions.handler;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.constants.ErrorCodes;
import org.acme.constants.ExceptionMessages;
import org.acme.model.ErrorResponse;
import org.acme.model.Violation;

import java.util.stream.Collectors;

@Provider
public class DataValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(jakarta.validation.ConstraintViolationException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                ExceptionMessages.CONSTRAINT_VIOLATION,
                ErrorCodes.CONSTRAINT_VIOLATION_ERROR_CODE,
                Response.Status.BAD_REQUEST.getStatusCode(),
                e.getConstraintViolations().stream()
                        .map(constraintViolation -> new Violation(constraintViolation.getPropertyPath().toString(),
                                constraintViolation.getMessage())).toList());

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}
