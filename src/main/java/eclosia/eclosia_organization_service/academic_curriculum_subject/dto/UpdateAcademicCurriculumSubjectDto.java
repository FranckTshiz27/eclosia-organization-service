package eclosia.eclosia_organization_service.academic_curriculum_subject.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateAcademicCurriculumSubjectDto {

    @NotNull(message = "Academic curriculum id is required")
    private UUID academicCurriculumId;

    @NotNull(message = "Subject id is required")
    private UUID subjectId;

    @NotNull(message = "Coefficient is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Coefficient must be greater than 0")
    private BigDecimal coefficient;

    @NotNull(message = "Maximum points is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum points must be greater than 0")
    private BigDecimal maximumPoints;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    private Boolean mandatory;

    private Boolean active;
}
