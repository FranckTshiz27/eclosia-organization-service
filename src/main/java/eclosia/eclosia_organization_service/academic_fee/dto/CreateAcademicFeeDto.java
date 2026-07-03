package eclosia.eclosia_organization_service.academic_fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateAcademicFeeDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    private Boolean payableByInstallment;

    private Boolean active;

    private String comment;

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Academic year id is required")
    private UUID academicYearId;

    @NotNull(message = "Fee category id is required")
    private UUID feeCategoryId;

    @NotNull(message = "Academic cycle id is required")
    private UUID academicCycleId;

    @NotNull(message = "Academic level id is required")
    private UUID academicLevelId;

    private UUID academicSectionId;

    private UUID academicOptionId;

    private UUID paymentInstallmentId;
}
