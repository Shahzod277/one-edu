package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.domain.Test;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends CrudRepository<Test, Long> {

    @Query("select t from Test t ")
    List<Test> findAll();

}
