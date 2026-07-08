package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentJournalRowDto {

    private String cycleName;
    private Integer cycleDisplayOrder;
    private Integer cyclePaymentCount;
    private String cycleTotalLabel;

    private String classroomName;
    private String feeCategoryName;
    private Integer categoryPaymentCount;
    private String categoryTotalLabel;
    private Integer classPaymentCount;
    private String classTotalLabel;
    private Integer rowNumber;
    private String paymentDate;
    private String createdAtLabel;
    private String receiptNumber;
    private String studentNumber;
    private String studentFullName;
    private String installmentLabel;
    private String paymentMethod;
    private String currencyCode;
    private String amountLabel;
    private BigDecimal amountValue;
    private BigDecimal amountUsd;
    private String cashier;
    private String observation;
}
