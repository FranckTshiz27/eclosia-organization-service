package eclosia.eclosia_organization_service.student_period_score.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpsertStudentPeriodScoreDto {

    @NotNull(message = "Enrollment id is required")
    private UUID enrollmentId;

    @NotNull(message = "Academic period id is required")
    private UUID academicPeriodId;

    @NotNull(message = "Academic curriculum subject id is required")
    private UUID academicCurriculumSubjectId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Score must be greater than or equal to 0")
    private BigDecimal score;
}
