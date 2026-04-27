package uz.raqamli_talim.oneedu.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationRequest {

    @NotBlank(message = "Code bo'sh bo'lishi mumkin emas")
    private String code;

    @NotBlank(message = "Name bo'sh bo'lishi mumkin emas")
    private String name;

    private String ownership;
}
