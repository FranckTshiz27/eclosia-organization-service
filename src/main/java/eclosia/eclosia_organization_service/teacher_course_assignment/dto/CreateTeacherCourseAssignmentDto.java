package eclosia.eclosia_organization_service.teacher_course_assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateTeacherCourseAssignmentDto {

    @NotNull(message = "Teacher is required")
    private UUID teacherId;

    @NotNull(message = "School is required")
    private UUID schoolId;

    @NotNull(message = "Academic year is required")
    private UUID academicYearId;

    @NotNull(message = "Classroom is required")
    private UUID classroomId;

    @NotNull(message = "Subject is required")
    private UUID subjectId;

    private BigDecimal weeklyHours;

    private BigDecimal coefficient;

    @Size(max = 1000)
    private String remarks;
}
