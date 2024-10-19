package org.acme.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.constants.DataValidation;
import org.acme.constants.ValidationErrors;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class LoginRequest implements Serializable {

    @NotBlank(message = ValidationErrors.EMAIL_IS_MANDATORY)
    @Email(message = ValidationErrors.INVALID_EMAIL_FORMAT)
    private String email;
    @NotBlank(message = "Password is mandatory")
    @Pattern(regexp = DataValidation.PASSWORD_PATTERN, message = DataValidation.PASSWORD_REQUIREMENTS_MESSAGE)
    private String password;

}
