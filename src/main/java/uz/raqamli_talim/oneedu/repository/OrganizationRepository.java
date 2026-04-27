package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.raqamli_talim.oneedu.domain.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findAllByIsActiveTrue();

    Optional<Organization> findByIdAndIsActiveTrue(Long id);

    boolean existsByCode(String code);

    Optional<Organization> findByCode(String code);

    @Query("SELECT o FROM Organization o WHERE o.isActive = true " +
            "AND (:search IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(o.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Organization> findAllWithFilter(@Param("search") String search, Pageable pageable);
}
