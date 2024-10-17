package org.acme.model;

import jakarta.ws.rs.core.Response;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class ErrorResponse {

    private String message;
    private Response.Status status;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(String message, Response.Status status) {
        this.status = status;
        this.message = message;
    }

}
