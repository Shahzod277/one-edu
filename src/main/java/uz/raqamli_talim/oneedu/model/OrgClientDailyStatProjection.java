package uz.raqamli_talim.oneedu.model;

public interface OrgClientDailyStatProjection {
    Long getOrganizationId();
    String getOrganizationName();

    Long getClientSystemId();
//    String getApiKey();
    String getSystemName();
    String getDomen();

    java.time.LocalDate getDay();

    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Long getUniqueUsersSuccess();
}