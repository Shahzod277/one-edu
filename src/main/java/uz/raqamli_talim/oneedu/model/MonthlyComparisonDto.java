package uz.raqamli_talim.oneedu.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyComparisonDto {
    private String currentMonth;
    private long currentTotal;
    private long currentSuccess;
    private long currentError;
    private long currentUniqueUsers;

    private String previousMonth;
    private long previousTotal;
    private long previousSuccess;
    private long previousError;
    private long previousUniqueUsers;

    private double totalGrowthPercent;
    private double successGrowthPercent;
    private double errorGrowthPercent;
    private double uniqueUsersGrowthPercent;
}