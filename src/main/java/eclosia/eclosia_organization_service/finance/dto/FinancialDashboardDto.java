package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FinancialDashboardDto {

    private FinancialDashboardSummaryDto summary;
    private List<RevenueEvolutionItemDto> revenueEvolution;
    private List<RevenueByCategoryDto> revenueByCategory;
    private List<ClassPerformanceDto> classPerformance;
    private List<RevenueByPaymentMethodDto> revenueByPaymentMethod;
    private ArrearsSummaryDto arrearsSummary;
    private List<RecentPaymentDto> recentPayments;
    private QuickSummaryDto quickSummary;
}
