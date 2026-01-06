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
        @Index(name = "idx_users_pinfl", columnList = "pinfl", unique = true)})
public class User extends AbstractEntity {

    private String firstName;
    private String lastName;
    private String fatherName;
    private String password;
    private String pinfl;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(firstName, user.firstName) && Objects.equals(lastName, user.lastName) && Objects.equals(fatherName, user.fatherName) && Objects.equals(password, user.password) && Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, fatherName, password, roles);
    }
}
