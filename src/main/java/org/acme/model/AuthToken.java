package org.acme.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken implements Serializable {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;

}
