package uz.raqamli_talim.oneedu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import uz.raqamli_talim.oneedu.sevice.AuditStatService;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl", modifyOnCreate = false)
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class OneEduApplication implements CommandLineRunner {
    @Autowired
    private AuditStatService service;

    public static void main(String[] args) {
        SpringApplication.run(OneEduApplication.class, args);
    }
    @Override
    public void run(String... args) throws Exception {
//        adminService.addUniversities();
//        adminService.addSpecialitiesBachelor();
//        adminService.addSpecialitiesMagistr();
//        adminService.addFaculty();
//        adminService.addCathedra();
//        adminService.updateUniversities();
//       publicService.accessUserUpdateCodeTeacher();
//       userService.checkStatus();
//        service.test();
    }
}
