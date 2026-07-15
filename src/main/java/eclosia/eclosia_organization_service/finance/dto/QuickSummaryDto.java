package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QuickSummaryDto {

    private long configuredFeesCount;
    private long configuredInstallmentsCount;
    private BigDecimal averageExpectedPerStudent;
    private BigDecimal averageCollectedPerStudent;
}
