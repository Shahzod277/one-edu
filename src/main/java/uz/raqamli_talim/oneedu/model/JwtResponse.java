package uz.raqamli_talim.oneedu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private Integer id;
    private String username;
    private String jwtToken;
    private List<String> roles = new ArrayList<>();

    public JwtResponse(String username, String jwtToken, List<String> role) {
        this.username = username;
        this.jwtToken = jwtToken;
        this.roles = role;
    }
}
