package uz.raqamli_talim.oneedu.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test")
@Getter
@Setter
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String pinfl;
    private String universityCode;
    private boolean hasError;
    @Column(columnDefinition = "text")
    private String error;
}
