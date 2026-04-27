package uz.raqamli_talim.oneedu.model;

public interface MonthlyTrendProjection {
    String getMonth();
    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
    Long getUniqueUsers();
}