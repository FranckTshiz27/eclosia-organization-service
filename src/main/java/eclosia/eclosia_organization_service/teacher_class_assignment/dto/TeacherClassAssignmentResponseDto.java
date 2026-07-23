package eclosia.eclosia_organization_service.teacher_class_assignment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TeacherClassAssignmentResponseDto {

    private UUID id;
    private UUID teacherId;
    private String teacherFullName;
    private UUID schoolId;
    private String schoolName;
    private UUID academicYearId;
    private String academicYearCode;
    private String academicYearName;
    private UUID classroomId;
    private String classroomDisplayName;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
