package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.raqamli_talim.oneedu.model.OrgClientDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgClientMonthlyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgMonthlyStatProjection;
import uz.raqamli_talim.oneedu.repository.AuditRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditStatService {

    private final AuditRepository repo;

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
}