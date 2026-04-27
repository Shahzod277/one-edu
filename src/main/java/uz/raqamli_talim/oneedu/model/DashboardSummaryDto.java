package uz.raqamli_talim.oneedu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalOrganizations;
    private long activeClientSystems;

    private long todayTotal;
    private long todaySuccess;
    private long todayError;
    private long todayUniqueUsers;

    private long allTimeTotal;
    private long allTimeSuccess;
    private long allTimeError;
    private long allTimeUniqueUsers;
}
