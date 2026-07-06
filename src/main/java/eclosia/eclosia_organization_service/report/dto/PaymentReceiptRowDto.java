package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

@Data
public class PaymentReceiptRowDto {

    private Integer rowNumber;
    private String feeLabel;
    private String periodDetail;
    private String unitAmount;
    private Integer quantity;
    private String paidAmount;
}
