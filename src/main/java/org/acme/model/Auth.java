package org.acme.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.entity.User;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Auth implements Serializable {

    private String email;
    private String password;
    private String firstName;
    private String lastName;

    public Auth(User user) {
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }
}
