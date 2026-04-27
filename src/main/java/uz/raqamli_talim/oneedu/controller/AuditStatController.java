package uz.raqamli_talim.oneedu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.raqamli_talim.oneedu.model.*;
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

    // ===== DASHBOARD =====

    @GetMapping("/dashboard/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public DashboardSummaryDto summary() {
        return service.summary();
    }

    @GetMapping("/dashboard/monthly-trend")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<MonthlyTrendProjection> monthlyTrend(
            @RequestParam(defaultValue = "6") int months
    ) {
        return service.monthlyTrend(months);
    }

    @GetMapping("/dashboard/monthly-comparison")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public MonthlyComparisonDto monthlyComparison() {
        return service.monthlyComparison();
    }

    @GetMapping("/dashboard/top-organizations")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<TopOrganizationProjection> topOrganizations(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return service.topOrganizations(limit);
    }

    @GetMapping("/dashboard/top-error-organizations")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<TopOrganizationProjection> topErrorOrganizations(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return service.topErrorOrganizations(limit);
    }

    @GetMapping("/dashboard/unique-users-trend")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<UniqueUsersTrendProjection> uniqueUsersTrend(
            @RequestParam(defaultValue = "6") int months
    ) {
        return service.uniqueUsersTrend(months);
    }

    @GetMapping("/dashboard/peak-hours")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<PeakHourProjection> peakHours() {
        return service.peakHours();
    }

    @GetMapping("/dashboard/error-breakdown")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<ErrorBreakdownProjection> errorBreakdown(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return service.errorBreakdown(limit);
    }

    @GetMapping("/dashboard/client-uptime")
    @PreAuthorize("isAuthenticated()")
    @Operation(security = {@SecurityRequirement(name = "bearer-key")})
    public List<ClientUptimeProjection> clientUptime() {
        return service.clientUptime();
    }

    @PostMapping("test")
    public HemisAuthConfigService.TokenData test(
            @RequestParam(value = "pinfl", required = false) String pinfl,
            @RequestParam(value = "serialNumber", required = false) String seralNumber
    ) {
        return hemisAuthConfigService.eduIdLoginTest(pinfl, seralNumber);
    }

    @PostMapping("test-employee")
    public HemisAuthConfigService.TokenData testEmployee(
            @RequestParam(value = "pinfl", required = false) String pinfl,
            @RequestParam(value = "serialNumber", required = false) String seralNumber,
            @RequestParam(value = "universityCode", required = false) String universityCode,
            @RequestParam(value = "type", required = false) String type
    ) {
        return hemisAuthConfigService.eduIdLoginEmployeeTest(pinfl, seralNumber,universityCode,type);
    }
}