package uz.raqamli_talim.oneedu.repository;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uz.raqamli_talim.oneedu.domain.ClientSystem;

import java.util.Optional;

public interface ClientSystemRepository extends CrudRepository<ClientSystem, Long> {
    @Query("select c from ClientSystem  c where c.active=true  and c.apiKey=?1")
    Optional<ClientSystem> findByApiKey(String apiKey);

    @Query("""
        select c
        from ClientSystem c
        left join c.organization o
        where (:active is null or c.active = :active)
          and (:organizationId is null or o.id = :organizationId)
          and (:isPushed is null or c.isPushed = :isPushed)
          and (
                :search is null or :search = '' or
                lower(c.domen) like lower(concat('%', :search, '%')) or
                lower(c.systemName) like lower(concat('%', :search, '%')) or
                lower(o.name) like lower(concat('%', :search, '%'))
          )
        """)
    Page<ClientSystem> findAllWithFilter(
            @Param("organizationId") Long organizationId,
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("isPushed") Boolean isPushed,
            Pageable pageable
    );
    @Query("""
    select case when count(c) > 0 then true else false end
    from ClientSystem c
    where c.active = true
      and c.organization.id = ?1
""")
boolean existsActiveByOrganizationId(Long organizationId);
    boolean existsByApiKey(String apiKey);




}
