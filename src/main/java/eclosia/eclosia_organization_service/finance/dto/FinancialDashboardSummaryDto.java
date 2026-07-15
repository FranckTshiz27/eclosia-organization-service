package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FinancialDashboardSummaryDto {

    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal recoveryRate;
    private long totalStudents;
    private long totalClasses;
    private BigDecimal todayCollectedAmount;
    private long todayPaymentCount;
}
