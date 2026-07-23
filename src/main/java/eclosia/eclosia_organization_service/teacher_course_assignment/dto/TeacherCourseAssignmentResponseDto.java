package eclosia.eclosia_organization_service.teacher_course_assignment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TeacherCourseAssignmentResponseDto {

    private UUID id;
    private UUID teacherId;
    private String teacherFullName;
    private UUID schoolId;
    private String schoolName;
    private UUID academicYearId;
    private String academicYearCode;
    private UUID classroomId;
    private String classroomDisplayName;
    private UUID subjectId;
    private String subjectCode;
    private String subjectName;
    private BigDecimal weeklyHours;
    private BigDecimal coefficient;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
