package eclosia.eclosia_organization_service.teacher.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eclosia.eclosia_organization_service.common.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherResponseDto {

    private UUID id;
    private UUID securityUserId;

    // Compte (exposé à la création pour transmission admin)
    private String username;
    private String temporaryPassword;

    // Informations personnelles
    private String firstName;
    private String lastName;
    private String middleName;
    private Gender gender;
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String address;

    // Informations professionnelles
    private UUID schoolId;
    private String schoolName;
    private String registrationNumber;
    private LocalDate hiringDate;
    private LocalDate leavingDate;
    private String qualification;
    private String specialty;
    private String grade;
    private String signature;
    private Boolean titular;
    private Boolean active;
    private String remarks;

    private List<TeacherRoleDto> roles;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
