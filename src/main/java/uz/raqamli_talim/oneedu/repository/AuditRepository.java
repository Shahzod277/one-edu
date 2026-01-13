package uz.raqamli_talim.oneedu.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uz.raqamli_talim.oneedu.domain.Audit;
import uz.raqamli_talim.oneedu.domain.ClientSystem;
import uz.raqamli_talim.oneedu.model.OrgClientDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgClientMonthlyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgDailyStatProjection;
import uz.raqamli_talim.oneedu.model.OrgMonthlyStatProjection;

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
}