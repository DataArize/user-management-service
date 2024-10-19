package org.acme.utils;

import jakarta.ws.rs.core.Response;
import lombok.experimental.UtilityClass;
import org.acme.model.ErrorResponse;

@UtilityClass
public class ErrorResponseUtils {

    /**
     * Creates a custom error response with the specified details.
     *
     * This method constructs an {@link ErrorResponse} object using the provided
     * error message, HTTP status, error title, and error code. It then builds
     * a {@link Response} object with the specified status and the error response
     * entity.
     *
     * @param message   the error message to be included in the response.
     * @param status    the HTTP status code to be set for the response.
     * @param title     a brief title describing the error.
     * @param errorCode a specific code identifying the error type.
     * @return          a {@link Response} object containing the error details.
     */
    public static Response createErrorResponse(String message, Response.Status status, String title, String errorCode) {
        ErrorResponse errorResponse = new ErrorResponse(message, status.getStatusCode(), title, errorCode);
        return Response.status(status).entity(errorResponse).build();
    }

}
