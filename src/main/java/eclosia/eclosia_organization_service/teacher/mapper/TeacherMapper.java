package eclosia.eclosia_organization_service.teacher.mapper;

import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.teacher.dto.TeacherResponseDto;
import eclosia.eclosia_organization_service.teacher.dto.TeacherRoleDto;
import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import eclosia.eclosia_organization_service.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Mapper manuel (MapStruct non utilisé dans ce projet).
 */
@Component
public class TeacherMapper {

    public TeacherResponseDto toResponse(Teacher teacher) {
        return toResponse(teacher, null);
    }

    public TeacherResponseDto toResponse(Teacher teacher, String temporaryPassword) {
        User user = teacher.getSecurityUser();

        return TeacherResponseDto.builder()
                .id(teacher.getId())
                .securityUserId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : null)
                .temporaryPassword(temporaryPassword)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .middleName(user != null ? user.getMiddleName() : null)
                .gender(user != null ? user.getGender() : null)
                .birthDate(user != null ? user.getBirthDate() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .address(user != null ? user.getAddress() : null)
                .schoolId(teacher.getSchool() != null ? teacher.getSchool().getId() : null)
                .schoolName(teacher.getSchool() != null ? teacher.getSchool().getName() : null)
                .registrationNumber(teacher.getRegistrationNumber())
                .hiringDate(teacher.getHiringDate())
                .leavingDate(teacher.getLeavingDate())
                .qualification(teacher.getQualification())
                .specialty(teacher.getSpecialty())
                .grade(teacher.getGrade())
                .signature(teacher.getSignature())
                .titular(teacher.getTitular())
                .active(teacher.getActive())
                .remarks(teacher.getRemarks())
                .roles(mapRoles(user))
                .createdAt(teacher.getCreatedAt())
                .updatedAt(teacher.getUpdatedAt())
                .build();
    }

    private List<TeacherRoleDto> mapRoles(User user) {
        if (user == null || user.getRoles() == null) {
            return List.of();
        }
        return user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Role::getCode))
                .map(role -> TeacherRoleDto.builder()
                        .id(role.getId())
                        .code(role.getCode())
                        .name(role.getName())
                        .build())
                .toList();
    }
}
