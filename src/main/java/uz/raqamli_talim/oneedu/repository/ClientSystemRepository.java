package uz.raqamli_talim.oneedu.repository;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uz.raqamli_talim.oneedu.domain.ClientSystem;

import java.util.Optional;

public interface ClientSystemRepository extends CrudRepository<ClientSystem, Long> {
    @Query("select c from ClientSystem  c where c.active=true  and c.apiKey=?1")
    Optional<ClientSystem> findByApiKey(String apiKey);
    @Query("select c from ClientSystem c ")
    Page<ClientSystem> findAllActive(Pageable pageable);
}
