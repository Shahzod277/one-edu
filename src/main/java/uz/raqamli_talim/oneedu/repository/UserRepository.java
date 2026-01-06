package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.raqamli_talim.oneedu.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select u from User u where u.pinfl = ?1 and u.isActive = true ")
    @EntityGraph(attributePaths = "roles", type = EntityGraph.EntityGraphType.LOAD)
    Optional<User> findUserByPinflForSignIn(String pinfl);

    @Query("select u from User u where u.pinfl = ?1 and u.isActive is true ")
    Optional<User> findActiveUserByPinfl(String pinfl);

    @Query("select u from User u where u.pinfl = ?1 ")
    Optional<User> findByPinfl(String pinfl);

    @Query("select (count(u) > 0) from User u where u.phoneNumber = ?1")
    Boolean existsByPhoneNumber(String phoneNumber);
//
//    @Query(value = " select u.id, u.first_name firstName, u.last_name lastName, u.father_name fatherName, " +
//            "       u.pinfl, u.phone_number phoneNumber, u.serial_and_number serialAndNumber, " +
//            "       u.birth_date birthDate, u.current_role_name currentRoleName, u.current_role_id, " +
//            "       u.institution_id, u.organization_id, u.organization_type" +
//            "       from users u where u.pinfl = ?1 and u.is_active is true ", nativeQuery = true)
//    Optional<UserInfoProjection> findActiveUserInfoByPinfl(String pinfl);

    @Query("select u from User u join fetch u.roles where u.pinfl = ?1 and u.isActive = true ")
    Optional<User> findAdminInfoByPinfl(String pinfl);

//    @Query(
//            value = "SELECT DISTINCT u.id, " +
//                    "  TO_CHAR(u.created_at, 'YYYY-MM-DD') AS createDate, " +
//                    "  u.pinfl, " +
//                    " CONCAT(u.last_name, ' ', u.first_name, ' ', u.father_name, ' ') AS fullName, " +
//                    "  u.phone_number phoneNumber," +
//                    "  u.is_active isActive," +
//                    "  org.name AS organization, u.organization_type organizationType " +
//                    "FROM users u " +
//                    "INNER JOIN users_role ur ON u.id = ur.user_id " +
//                    "LEFT JOIN organization org ON u.organization_id = org.id " +
//                    "WHERE (?1 IS NULL OR u.organization_id = ?1) " +
//                    "  AND (?2 IS NULL OR u.is_active = ?2) " +
//                    "  AND (?3 IS NULL OR ur.role_id = ?3) " +
//                    "  AND (?4 IS NULL OR CONCAT(u.pinfl, u.last_name, ' ', u.first_name, ' ', u.father_name, ' ', u.phone_number) ILIKE CONCAT('%', ?4, '%'))",
//
//            countQuery = "SELECT COUNT(DISTINCT u.id) " +
//                    "FROM users u " +
//                    "INNER JOIN users_role ur ON u.id = ur.user_id " +
//                    "LEFT JOIN organization org ON u.organization_id = org.id " +
//                    "WHERE (?1 IS NULL OR u.organization_id = ?1) " +
//                    "  AND (?2 IS NULL OR u.is_active = ?2) " +
//                    "  AND (?3 IS NULL OR ur.role_id = ?3) " +
//                    "  AND (?4 IS NULL OR CONCAT(u.pinfl, u.last_name, ' ', u.first_name, ' ', u.father_name, ' ', u.phone_number) ILIKE CONCAT('%', ?4, '%'))",
//
//            nativeQuery = true
//    )
//    Page<ORGAdminShortInfoProjection> getAllAdmins(
//            Integer organizationId,
//            Boolean isActive,
//            Integer roleId,
//            String search,
//            Pageable pageable
//    );

}


