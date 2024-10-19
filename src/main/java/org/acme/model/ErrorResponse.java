package org.acme.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.Response;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {


    private String title;
    private String message;
    private String errorCode;
    private int status;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<Violation> violations;


    public ErrorResponse(String message, int status,
                         String title, String errorCode) {
        this.status = status;
        this.message = message;
        this.title = title;
        this.errorCode = errorCode;
    }

    public ErrorResponse(String title, String errorCode,
                         int status, List<Violation> violations) {
        this.title = title;
        this.errorCode = errorCode;
        this.status = status;
        this.violations = violations;
    }

}
