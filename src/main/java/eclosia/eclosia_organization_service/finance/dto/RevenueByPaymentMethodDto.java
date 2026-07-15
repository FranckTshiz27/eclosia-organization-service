package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueByPaymentMethodDto {

    private String paymentMethod;
    private BigDecimal collectedAmount;
    private BigDecimal percentage;
}
