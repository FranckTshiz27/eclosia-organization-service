package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ArrearsSummaryDto {

    private long unpaidStudentCount;
    private BigDecimal remainingAmount;
    private BigDecimal remainingPercentage;
    private BigDecimal recoveryRate;
}
