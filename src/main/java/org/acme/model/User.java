package org.acme.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.constants.AccountStatus;
import org.acme.entity.Role;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private String email;
    private String firstName;
    private String lastName;
    private List<Role> roles = new ArrayList<>();
    private AccountStatus status;
    private String quota;
    private LocalDateTime lastLogin;
}
