package uz.raqamli_talim.oneedu.sevice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.raqamli_talim.oneedu.domain.Test;
import uz.raqamli_talim.oneedu.model.*;
import uz.raqamli_talim.oneedu.repository.AuditRepository;
import uz.raqamli_talim.oneedu.repository.ClientSystemRepository;
import uz.raqamli_talim.oneedu.repository.OrganizationRepository;
import uz.raqamli_talim.oneedu.repository.TestRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditStatService {

    private final AuditRepository repo;
    private final AuthService authService;
    private final TestRepository testRepository;
    private final HemisAuthConfigService hemisAuthConfigService;
    private final OrganizationRepository organizationRepository;
    private final ClientSystemRepository clientSystemRepository;

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


    // ===== DASHBOARD =====

    @Transactional(readOnly = true)
    public DashboardSummaryDto summary() {
        return new DashboardSummaryDto(
                organizationRepository.count(),
                clientSystemRepository.countByActiveTrue(),
                repo.countTodayTotal(),
                repo.countTodaySuccess(),
                repo.countTodayError(),
                repo.countTodayUniqueUsers(),
                repo.countAllTotal(),
                repo.countAllSuccess(),
                repo.countAllError(),
                repo.countAllUniqueUsers()
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendProjection> monthlyTrend(int months) {
        return repo.monthlyTrend(months);
    }

    @Transactional(readOnly = true)
    public MonthlyComparisonDto monthlyComparison() {
        List<MonthlyTrendProjection> trend = repo.monthlyTrend(2);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        String currentMonthStr = LocalDate.now().format(fmt);
        String previousMonthStr = LocalDate.now().minusMonths(1).format(fmt);

        MonthlyTrendProjection current = null;
        MonthlyTrendProjection previous = null;

        for (MonthlyTrendProjection t : trend) {
            if (currentMonthStr.equals(t.getMonth())) current = t;
            if (previousMonthStr.equals(t.getMonth())) previous = t;
        }

        long curTotal = current != null ? current.getTotalLogs() : 0;
        long curSuccess = current != null ? current.getSuccessLogs() : 0;
        long curError = current != null ? current.getErrorLogs() : 0;
        long curUsers = current != null ? current.getUniqueUsers() : 0;

        long prevTotal = previous != null ? previous.getTotalLogs() : 0;
        long prevSuccess = previous != null ? previous.getSuccessLogs() : 0;
        long prevError = previous != null ? previous.getErrorLogs() : 0;
        long prevUsers = previous != null ? previous.getUniqueUsers() : 0;

        return new MonthlyComparisonDto(
                currentMonthStr, curTotal, curSuccess, curError, curUsers,
                previousMonthStr, prevTotal, prevSuccess, prevError, prevUsers,
                growthPercent(prevTotal, curTotal),
                growthPercent(prevSuccess, curSuccess),
                growthPercent(prevError, curError),
                growthPercent(prevUsers, curUsers)
        );
    }

    @Transactional(readOnly = true)
    public List<TopOrganizationProjection> topOrganizations(int limit) {
        return repo.topOrganizations(limit);
    }

    @Transactional(readOnly = true)
    public List<TopOrganizationProjection> topErrorOrganizations(int limit) {
        return repo.topErrorOrganizations(limit);
    }

    @Transactional(readOnly = true)
    public List<UniqueUsersTrendProjection> uniqueUsersTrend(int months) {
        return repo.uniqueUsersTrend(months);
    }

    @Transactional(readOnly = true)
    public List<PeakHourProjection> peakHours() {
        return repo.peakHours();
    }

    @Transactional(readOnly = true)
    public List<ErrorBreakdownProjection> errorBreakdown(int limit) {
        return repo.errorBreakdown(limit);
    }

    @Transactional(readOnly = true)
    public List<ClientUptimeProjection> clientUptime() {
        return repo.clientUptime();
    }

    private double growthPercent(long previous, long current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((current - previous) * 10000.0 / previous) / 100.0;
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