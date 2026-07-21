package eclosia.eclosia_organization_service.academic_period.dto;

import eclosia.eclosia_organization_service.academic_period.enums.AcademicPeriodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAcademicPeriodDto {

    @NotNull(message = "Academic term id is required")
    private UUID academicTermId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Period type is required")
    private AcademicPeriodType periodType;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    @NotNull(message = "Maximum score ratio is required")
    @Min(value = 1, message = "Maximum score ratio must be greater than 0")
    private Integer maximumScoreRatio;

    private Boolean active;
}
