package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uz.raqamli_talim.oneedu.domain.Audit;
import uz.raqamli_talim.oneedu.domain.ClientSystem;

import java.util.Optional;

public interface AuditRepository extends CrudRepository<Audit, Long> {
}
