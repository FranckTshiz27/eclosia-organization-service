package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecoveryDashboardRowDto {

    // Group: cycle
    private String cycleName;
    private Integer cycleDisplayOrder;
    private Integer cycleEffectiveCount;
    private Integer cyclePaidCount;
    private Integer cycleUnpaidCount;
    private String cycleExpectedTotalLabel;
    private String cyclePaidTotalLabel;
    private String cycleRecoveryRateLabel;
    private String cycleRemainingTotalLabel;

    // Group: fee category (within cycle)
    private String feeCategoryName;
    private Integer categoryEffectiveCount;
    private String categoryExpectedTotalLabel;
    private String categoryPaidTotalLabel;
    private String categoryRecoveryRateLabel;
    private String categoryRemainingTotalLabel;
    private String categoryRemainingRateLabel;

    // Group: class within category
    private String classroomName;
    private Integer classTotalEnrollmentCount;
    private Integer classUnpaidCount;
    private Integer classPaidCount;
    private String classUnpaidTotalLabel;
    private Integer classEffectiveCount;
    private String classExpectedTotalLabel;
    private String classPaidTotalLabel;
    private String classRecoveryRateLabel;
    private String classRemainingTotalLabel;
    private String classRemainingRateLabel;

    // Detail: student row
    private Integer rowNumber;
    private String studentMatricule;
    private String studentFullName;

    private String expectedAmountLabel;
    private String paidAmountLabel;
    private String paidPercentageLabel;
    private String remainingAmountLabel;
    private String remainingPercentageLabel;

    private String paymentStatusCode;
    private String paymentStatusLabel;

    // Internal (not used by Jasper)
    private BigDecimal expectedAmount;
    private BigDecimal paidAmountReference;
    private BigDecimal remainingAmount;
}
