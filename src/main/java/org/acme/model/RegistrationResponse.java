package org.acme.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.entity.User;

@Data
@NoArgsConstructor
public class RegistrationResponse {

    private String email;
    private String firstName;
    private String lastName;

    public RegistrationResponse(User user) {
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }
}
