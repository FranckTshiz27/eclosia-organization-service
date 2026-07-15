package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueStatementRowDto {

    private Integer rowNumber;
    private String feeCategoryName;
    private Integer studentCount;
    private BigDecimal expectedAmount;
    private String expectedAmountLabel;
    private BigDecimal collectedAmount;
    private String collectedAmountLabel;
    private String recoveryRateLabel;
    private BigDecimal remainingAmount;
    private String remainingAmountLabel;

    private Integer categoryStudentCount;
    private String categoryExpectedAmountLabel;
    private String categoryCollectedAmountLabel;
    private String categoryRecoveryRateLabel;
    private String categoryRemainingAmountLabel;
}
