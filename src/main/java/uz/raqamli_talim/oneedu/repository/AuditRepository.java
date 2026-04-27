package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uz.raqamli_talim.oneedu.domain.Audit;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.model.*;

import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    // 1) Org daily
    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.code AS organizationCode,
            o.name AS organizationName,
            DATE(a.created_at) AS day,

            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsersSuccess
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        WHERE (:orgId IS NULL OR o.id = :orgId)
        GROUP BY o.id, o.code, o.name, DATE(a.created_at)
        ORDER BY day DESC, o.id
        """, nativeQuery = true)
    List<OrgDailyStatProjection> orgDaily(@Param("orgId") Long orgId);

    // 2) Org monthly
    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.code AS organizationCode,
            o.name AS organizationName,
            DATE_TRUNC('month', a.created_at) AS month,

            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsersSuccess
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        WHERE (:orgId IS NULL OR o.id = :orgId)
        GROUP BY o.id, o.code, o.name, DATE_TRUNC('month', a.created_at)
        ORDER BY month DESC, o.id
        """, nativeQuery = true)
    List<OrgMonthlyStatProjection> orgMonthly(@Param("orgId") Long orgId);

    // 3) Org -> client daily
    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.name AS organizationName,

            cs.id  AS clientSystemId,
            cs.api_key AS apiKey,
            cs.system_name AS systemName,
            cs.domen AS domen,

            DATE(a.created_at) AS day,

            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsersSuccess
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        WHERE (:orgId IS NULL OR o.id = :orgId)
        GROUP BY o.id, o.name, cs.id, cs.api_key, cs.system_name, cs.domen, DATE(a.created_at)
        ORDER BY day DESC, o.id, cs.id
        """, nativeQuery = true)
    List<OrgClientDailyStatProjection> orgClientDaily(@Param("orgId") Long orgId);

    // 4) Org -> client monthly
    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.name AS organizationName,

            cs.id  AS clientSystemId,
            cs.api_key AS apiKey,
            cs.system_name AS systemName,
            cs.domen AS domen,

            DATE_TRUNC('month', a.created_at) AS month,

            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsersSuccess
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        WHERE (:orgId IS NULL OR o.id = :orgId)
        GROUP BY o.id, o.name, cs.id, cs.api_key, cs.system_name, cs.domen, DATE_TRUNC('month', a.created_at)
        ORDER BY month DESC, o.id, cs.id
        """, nativeQuery = true)
    List<OrgClientMonthlyStatProjection> orgClientMonthly(@Param("orgId") Long orgId);

    // ===== DASHBOARD: Summary =====

    @Query(value = """
        SELECT COUNT(*) FROM audit a WHERE DATE(a.created_at) = CURRENT_DATE
        """, nativeQuery = true)
    long countTodayTotal();

    @Query(value = """
        SELECT COUNT(*) FROM audit a WHERE DATE(a.created_at) = CURRENT_DATE AND a.error = false
        """, nativeQuery = true)
    long countTodaySuccess();

    @Query(value = """
        SELECT COUNT(*) FROM audit a WHERE DATE(a.created_at) = CURRENT_DATE AND a.error = true
        """, nativeQuery = true)
    long countTodayError();

    @Query(value = """
        SELECT COUNT(DISTINCT a.pinfl) FROM audit a WHERE DATE(a.created_at) = CURRENT_DATE AND a.error = false
        """, nativeQuery = true)
    long countTodayUniqueUsers();

    @Query(value = "SELECT COUNT(*) FROM audit", nativeQuery = true)
    long countAllTotal();

    @Query(value = "SELECT COUNT(*) FROM audit WHERE error = false", nativeQuery = true)
    long countAllSuccess();

    @Query(value = "SELECT COUNT(*) FROM audit WHERE error = true", nativeQuery = true)
    long countAllError();

    @Query(value = "SELECT COUNT(DISTINCT pinfl) FROM audit WHERE error = false", nativeQuery = true)
    long countAllUniqueUsers();

    // ===== DASHBOARD: Monthly trend =====

    @Query(value = """
        SELECT
            TO_CHAR(DATE_TRUNC('month', a.created_at), 'YYYY-MM') AS month,
            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsers
        FROM audit a
        WHERE a.created_at >= DATE_TRUNC('month', CURRENT_DATE) - CAST(:months || ' months' AS INTERVAL)
        GROUP BY DATE_TRUNC('month', a.created_at)
        ORDER BY month
        """, nativeQuery = true)
    List<MonthlyTrendProjection> monthlyTrend(@Param("months") int months);

    // ===== DASHBOARD: Top organizations =====

    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.code AS organizationCode,
            o.name AS organizationName,
            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            ROUND(SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 2) AS successRate
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        GROUP BY o.id, o.code, o.name
        ORDER BY totalLogs DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TopOrganizationProjection> topOrganizations(@Param("limit") int limit);

    // ===== DASHBOARD: Top error organizations =====

    @Query(value = """
        SELECT
            o.id   AS organizationId,
            o.code AS organizationCode,
            o.name AS organizationName,
            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs,
            ROUND(SUM(CASE WHEN a.error = true THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 2) AS successRate
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        GROUP BY o.id, o.code, o.name
        ORDER BY errorLogs DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TopOrganizationProjection> topErrorOrganizations(@Param("limit") int limit);

    // ===== DASHBOARD: Unique users trend =====

    @Query(value = """
        SELECT
            TO_CHAR(DATE_TRUNC('month', a.created_at), 'YYYY-MM') AS month,
            COUNT(DISTINCT a.pinfl) FILTER (WHERE a.error = false) AS uniqueUsers
        FROM audit a
        WHERE a.created_at >= DATE_TRUNC('month', CURRENT_DATE) - CAST(:months || ' months' AS INTERVAL)
        GROUP BY DATE_TRUNC('month', a.created_at)
        ORDER BY month
        """, nativeQuery = true)
    List<UniqueUsersTrendProjection> uniqueUsersTrend(@Param("months") int months);

    // ===== DASHBOARD: Peak hours =====

    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM a.created_at)::int AS hour,
            COUNT(*) AS totalLogs,
            SUM(CASE WHEN a.error = false THEN 1 ELSE 0 END) AS successLogs,
            SUM(CASE WHEN a.error = true  THEN 1 ELSE 0 END) AS errorLogs
        FROM audit a
        GROUP BY EXTRACT(HOUR FROM a.created_at)
        ORDER BY hour
        """, nativeQuery = true)
    List<PeakHourProjection> peakHours();

    // ===== DASHBOARD: Error breakdown =====

    @Query(value = """
        SELECT
            COALESCE(a.error_massage, 'Noma''lum xato') AS errorMessage,
            COUNT(*) AS count,
            ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM audit WHERE error = true), 0), 2) AS percentage
        FROM audit a
        WHERE a.error = true
        GROUP BY a.error_massage
        ORDER BY count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ErrorBreakdownProjection> errorBreakdown(@Param("limit") int limit);

    // ===== DASHBOARD: Client uptime =====

    @Query(value = """
        SELECT
            cs.id AS clientSystemId,
            cs.system_name AS systemName,
            o.name AS organizationName,
            cs.domen AS domen,
            MAX(a.created_at) FILTER (WHERE a.error = false) AS lastSuccessAt,
            MAX(a.created_at) FILTER (WHERE a.error = true)  AS lastErrorAt,
            COUNT(*) AS totalLogs
        FROM audit a
        JOIN client_system cs ON cs.id = a.client_system_id
        JOIN organization  o  ON o.id  = cs.organization_id
        WHERE cs.active = true
        GROUP BY cs.id, cs.system_name, o.name, cs.domen
        ORDER BY lastSuccessAt DESC NULLS LAST
        """, nativeQuery = true)
    List<ClientUptimeProjection> clientUptime();
}