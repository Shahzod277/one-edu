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
    private Boolean active;

    private Long organizationId;
    private String organization;

    private String systemName;
    private String domen;
    private Boolean isUpdatedHemis;


    // 🔐 faqat public key
    private String privateKey;


}
