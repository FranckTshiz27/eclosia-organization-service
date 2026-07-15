package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class RevenueByCategoryDto {

    private UUID categoryId;
    private String categoryCode;
    private String categoryName;
    private BigDecimal collectedAmount;
    private BigDecimal percentage;
}
