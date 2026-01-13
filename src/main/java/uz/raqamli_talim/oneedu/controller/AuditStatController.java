package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.raqamli_talim.oneedu.model.OrgClientDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgClientMonthlyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgMonthlyStatProjection;
import uz.raqamli_talim.oneedu.sevice.AuditStatService;

import java.util.List;

@RestController
@RequestMapping("/api/audit-stats")
@RequiredArgsConstructor
public class AuditStatController {

    private final AuditStatService service;

    // 1) Org daily
    @GetMapping("/org/daily")
    public List<OrgDailyStatProjection> orgDaily(
            @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        return service.orgDaily(orgId);
    }

    // 2) Org monthly
    @GetMapping("/org/monthly")
    public List<OrgMonthlyStatProjection> orgMonthly(
            @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        return service.orgMonthly(orgId);
    }

    // 3) Org -> client daily
    @GetMapping("/org-client/daily")
    public List<OrgClientDailyStatProjection> orgClientDaily(
            @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        return service.orgClientDaily(orgId);
    }

    // 4) Org -> client monthly
    @GetMapping("/org-client/monthly")
    public List<OrgClientMonthlyStatProjection> orgClientMonthly(
            @RequestParam(value = "orgId", required = false) Long orgId
    ) {
        return service.orgClientMonthly(orgId);
    }
}