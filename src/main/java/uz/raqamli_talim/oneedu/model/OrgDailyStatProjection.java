package uz.raqamli_talim.oneedu.model;

import java.time.LocalDate;

public interface OrgDailyStatProjection {
    Long getOrganizationId();
    String getOrganizationCode();
    String getOrganizationName();
    LocalDate getDay();
    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Long getUniqueUsersSuccess();
}
