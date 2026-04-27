package uz.raqamli_talim.oneedu.model;

import java.time.LocalDateTime;

public interface ClientUptimeProjection {
    Long getClientSystemId();
    String getSystemName();
    String getOrganizationName();
    String getDomen();
    LocalDateTime getLastSuccessAt();
    LocalDateTime getLastErrorAt();
    Long getTotalLogs();
}
