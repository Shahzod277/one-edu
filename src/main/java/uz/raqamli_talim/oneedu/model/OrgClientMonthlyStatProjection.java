package uz.raqamli_talim.oneedu.model;

public interface OrgClientMonthlyStatProjection {
    Long getOrganizationId();
    String getOrganizationName();

    Long getClientSystemId();
//    String getApiKey();
    String getSystemName();
    String getDomen();

    java.time.LocalDateTime getMonth();

    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Long getUniqueUsersSuccess();
}