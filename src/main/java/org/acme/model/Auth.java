package org.acme.model;

import lombok.Data;

@Data
public class Auth {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
