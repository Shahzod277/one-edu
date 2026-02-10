package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.raqamli_talim.oneedu.domain.Test;
import uz.raqamli_talim.oneedu.model.OrgClientDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgClientMonthlyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgMonthlyStatProjection;
import uz.raqamli_talim.oneedu.repository.AuditRepository;
import uz.raqamli_talim.oneedu.repository.TestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditStatService {

    private final AuditRepository repo;
    private final AuthService authService;
    private final TestRepository testRepository;
    private final HemisAuthConfigService hemisAuthConfigService;

    @Transactional(readOnly = true)
    public List<OrgDailyStatProjection> orgDaily(Long orgId) {
        return repo.orgDaily(orgId);
    }

    @Transactional(readOnly = true)
    public List<OrgMonthlyStatProjection> orgMonthly(Long orgId) {
        return repo.orgMonthly(orgId);
    }

    @Transactional(readOnly = true)
    public List<OrgClientDailyStatProjection> orgClientDaily(Long orgId) {
        return repo.orgClientDaily(orgId);
    }

    @Transactional(readOnly = true)
    public List<OrgClientMonthlyStatProjection> orgClientMonthly(Long orgId) {
        return repo.orgClientMonthly(orgId);
    }


    public void test() {
testRepository.findAll();
        testRepository.findAll().parallelStream()
                .forEach(t -> {
                    try {
                        var tokenData =
                                hemisAuthConfigService.eduIdLoginTest(t.getPinfl(), t.getPinfl());
                        t.setHasError(Boolean.FALSE);

                        t.setError("OK token=" + tokenData.token());

                    } catch (Exception e) {
                        String msg = authService.extractHemisErrorMessage(e);
                        t.setHasError(Boolean.TRUE);
                        t.setError("ERR: " + msg);
                    }

                    // har birini alohida saqlaydi
                    testRepository.save(t);
                });
    }



}