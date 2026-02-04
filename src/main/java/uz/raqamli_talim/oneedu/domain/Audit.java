package uz.raqamli_talim.oneedu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit")
@Getter
@Setter
public class Audit extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private ClientSystem clientSystem;
    private String pinfl;
    private String serialNumber;
    private String universityCode;
    private Boolean error;
    @Column(columnDefinition = "text")
    private String errorMassage;

}
