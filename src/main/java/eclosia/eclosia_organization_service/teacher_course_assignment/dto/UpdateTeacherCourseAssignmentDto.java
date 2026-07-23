package eclosia.eclosia_organization_service.teacher_course_assignment.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateTeacherCourseAssignmentDto {

    private BigDecimal weeklyHours;

    private BigDecimal coefficient;

    @Size(max = 1000)
    private String remarks;

    private Boolean active;
}
