package uz.raqamli_talim.oneedu.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_phone_number", columnList = "phone_number", unique = true),
        @Index(name = "idx_users_pinfl", columnList = "pinfl", unique = true),
        @Index(name = "idx_users_current_role_id", columnList = "currentRoleId")})
public class User extends AbstractEntity {

    private String firstName;
    private String lastName;
    private String fatherName;
    private String pinfl;
    private String givenDate;
    private String photo;
    private String birthDate;
    @Column(length = 15)
    private String serialAndNumber;
    @Column(length = 14)
    private String phoneNumber;
    private String password;
    private Integer currentRoleId;
    private String currentRoleName;
    private String address;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_role",
            joinColumns = @JoinColumn(name = "user_id",
                    foreignKey = @ForeignKey(name = "FK_USER_ROLE_USER")),
            inverseJoinColumns = @JoinColumn(name = "role_id",
                    foreignKey = @ForeignKey(name = "FK_USER_ROLE_ROLE")))
    private Set<Role> roles = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(pinfl, user.pinfl) && Objects.equals(serialAndNumber, user.serialAndNumber) && Objects.equals(phoneNumber, user.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pinfl, serialAndNumber, phoneNumber);
    }
}
