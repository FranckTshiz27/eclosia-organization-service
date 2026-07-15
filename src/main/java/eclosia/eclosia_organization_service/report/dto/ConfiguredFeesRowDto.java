package eclosia.eclosia_organization_service.report.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfiguredFeesRowDto {

    private Integer rowNumber;
    private String code;
    private String feeName;
    private String feeCategoryName;
    private String applicableToLabel;
    private BigDecimal annualAmount;
    private String annualAmountLabel;
    private Integer installmentCount;
    private String periodicityLabel;
    private String calculationModeLabel;
    private String statusLabel;
    private Boolean active;

    private String cycleName;
    private Integer cycleDisplayOrder;
    private String installmentName;
    private Integer installmentDisplayOrder;

    private Integer categoryFeeCount;
    private String categoryTotalAmountLabel;
    private Integer categoryInstallmentCount;

    private Integer cycleFeeCount;
    private String cycleTotalAmountLabel;

    private Integer trancheFeeCount;
    private String trancheTotalAmountLabel;
}
