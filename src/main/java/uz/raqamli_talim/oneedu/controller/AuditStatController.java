package uz.raqamli_talim.oneedu.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.OrgClientDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgClientMonthlyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgMonthlyStatProjection;
import uz.raqamli_talim.oneedu.sevice.AuditStatService;
import uz.raqamli_talim.oneedu.sevice.HemisAuthConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/audit-stats")
@RequiredArgsConstructor
public class AuditStatController {

    private final AuditStatService service;
    private final HemisAuthConfigService hemisAuthConfigService;

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

    @PostMapping("test")
    public HemisAuthConfigService.TokenData test(
            @RequestParam(value = "pinfl", required = false) String pinfl,
            @RequestParam(value = "serialNumber", required = false) String seralNumber
    ) {
        return hemisAuthConfigService.eduIdLoginTest(pinfl, seralNumber);
    }
}