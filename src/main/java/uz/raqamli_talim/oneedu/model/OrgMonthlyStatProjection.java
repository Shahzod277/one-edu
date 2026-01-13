package uz.raqamli_talim.oneedu.model;

public interface OrgMonthlyStatProjection {
    Long getOrganizationId();
    String getOrganizationCode();
    String getOrganizationName();
    java.time.LocalDateTime getMonth(); // date_trunc -> timestamp

    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Long getUniqueUsersSuccess();
}