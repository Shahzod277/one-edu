package uz.raqamli_talim.oneedu.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientSystemRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    private String redirectUrl;

    private String domen;

    @NotBlank
    private String systemName;

    // create’da default true bo‘lsin desang, buni umuman olib tashlash ham mumkin
    private Boolean active;
    private Boolean isPushed;
}
