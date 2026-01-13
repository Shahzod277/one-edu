package uz.raqamli_talim.oneedu.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime isUpdatedHemisTime;


    // 🔐 faqat public key
    private String privateKey;


}
