package uz.raqamli_talim.oneedu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "organization", indexes = {
        @Index(name = "idx_organization_code", columnList = "code", unique = true),
        @Index(name = "idx_organization_name", columnList = "name")
})
@Getter
@Setter
@ToString(exclude = {})
public class Organization extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ownership")
    private String ownership;
}
