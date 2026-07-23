package eclosia.eclosia_organization_service.teacher_course_assignment.mapper;

import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.TeacherCourseAssignmentResponseDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.entity.TeacherCourseAssignment;
import eclosia.eclosia_organization_service.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherCourseAssignmentMapper {

    private final ClassroomNamingService classroomNamingService;

    public TeacherCourseAssignmentResponseDto toResponse(TeacherCourseAssignment assignment) {
        User user = assignment.getTeacher() != null ? assignment.getTeacher().getSecurityUser() : null;

        return TeacherCourseAssignmentResponseDto.builder()
                .id(assignment.getId())
                .teacherId(assignment.getTeacher() != null ? assignment.getTeacher().getId() : null)
                .teacherFullName(buildFullName(user))
                .schoolId(assignment.getSchool() != null ? assignment.getSchool().getId() : null)
                .schoolName(assignment.getSchool() != null ? assignment.getSchool().getName() : null)
                .academicYearId(assignment.getAcademicYear() != null ? assignment.getAcademicYear().getId() : null)
                .academicYearCode(assignment.getAcademicYear() != null ? assignment.getAcademicYear().getCode() : null)
                .classroomId(assignment.getClassroom() != null ? assignment.getClassroom().getId() : null)
                .classroomDisplayName(
                        assignment.getClassroom() != null
                                ? classroomNamingService.build(assignment.getClassroom())
                                : null
                )
                .subjectId(assignment.getSubject() != null ? assignment.getSubject().getId() : null)
                .subjectCode(assignment.getSubject() != null ? assignment.getSubject().getCode() : null)
                .subjectName(assignment.getSubject() != null ? assignment.getSubject().getName() : null)
                .weeklyHours(assignment.getWeeklyHours())
                .coefficient(assignment.getCoefficient())
                .active(assignment.getActive())
                .remarks(assignment.getRemarks())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private String buildFullName(User user) {
        if (user == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) {
            sb.append(user.getFirstName());
        }
        if (user.getMiddleName() != null && !user.getMiddleName().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(user.getMiddleName());
        }
        if (user.getLastName() != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(user.getLastName());
        }
        return sb.toString().trim();
    }
}
