package uz.raqamli_talim.oneedu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientSystemDto {
    private Long id;
    private String apiKey;
    private String redirectUrl;
    private String postCallbackUrl;
    private Boolean active;
    private String orgInn;
    private String org;
    private String systemName;
    private String domen;
}
