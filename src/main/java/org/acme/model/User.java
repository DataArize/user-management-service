package org.acme.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.constants.AccountStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles = new ArrayList<>();
    private AccountStatus status;
    private String quota;
}
