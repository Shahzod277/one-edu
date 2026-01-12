package uz.raqamli_talim.oneedu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization extends AbstractEntity {
    private String code;
    private String name;
}
