package uz.raqamli_talim.oneedu.model;

public interface TopOrganizationProjection {
    Long getOrganizationId();
    String getOrganizationCode();
    String getOrganizationName();
    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Double getSuccessRate();
}
