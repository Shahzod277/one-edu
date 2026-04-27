package uz.raqamli_talim.oneedu.model;

public interface PeakHourProjection {
    Integer getHour();
    Long getTotalLogs();
    Long getSuccessLogs();
    Long getErrorLogs();
}
