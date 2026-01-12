package uz.raqamli_talim.oneedu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "client_system")
@Getter
@Setter
public class ClientSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Masalan: KASBIY, HEMIS, DMCS
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;
    private String publicKey;
    private String privateKey;

    // Browser redirect (login tugagach)
    @Column(name = "redirect_url", nullable = false)
    private String redirectUrl;

    // Backend → backend POST qilinadigan URL
    @Column(name = "post_callback_url", nullable = false)
    private String postCallbackUrl;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "system_name")
    private String systemName;

    @Column(name = "domen")
    private String domen;
    @ManyToOne(fetch = FetchType.LAZY)
    private Organization organization;

}
