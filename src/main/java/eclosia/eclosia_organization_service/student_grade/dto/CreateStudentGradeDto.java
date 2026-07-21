package eclosia.eclosia_organization_service.student_grade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateStudentGradeDto {

    @NotNull(message = "Student enrollment id is required")
    private UUID studentEnrollmentId;

    @NotNull(message = "Academic period id is required")
    private UUID academicPeriodId;

    @NotNull(message = "Academic curriculum subject id is required")
    private UUID academicCurriculumSubjectId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Score must be greater than or equal to 0")
    private BigDecimal score;

    @Size(max = 500, message = "Observation must not exceed 500 characters")
    private String observation;

    private Boolean published;
}
