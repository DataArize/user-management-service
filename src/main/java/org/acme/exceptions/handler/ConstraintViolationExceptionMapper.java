package org.acme.exceptions.handler;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.model.ErrorResponse;
import org.hibernate.HibernateException;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<HibernateException> {

    @Override
    public Response toResponse(HibernateException exception) {

        Throwable cause = exception.getCause();
        if (cause instanceof ConstraintViolationException constraintEx) {
            StringBuilder message = new StringBuilder("Validation failed: \n");
            constraintEx.getConstraintViolations().forEach(violation ->
                message.append("Field: ")
                        .append(violation.getPropertyPath())
                        .append(" - ")
                        .append(violation.getMessage())
                        .append("\n")
            );
            ErrorResponse errorResponse = new ErrorResponse(message.toString(), Response.Status.BAD_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        ErrorResponse errorResponse = new ErrorResponse("Database error occurred.", Response.Status.INTERNAL_SERVER_ERROR);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();

    }
}
