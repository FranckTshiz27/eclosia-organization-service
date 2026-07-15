package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueEvolutionItemDto {

    private String period;
    private BigDecimal collectedAmount;
}
