package eclosia.eclosia_organization_service.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateEnrollmentDto {

    @NotBlank
    private String lastName;

    @NotBlank
    private String middleName;

    @NotBlank
    private String firstName;

    @NotBlank
    private String gender;

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private UUID birthCountryId;

    private UUID birthCityId;

    private UUID birthCommuneId;

    private String nationality;

    private UUID countryId;

    private UUID cityId;

    private UUID communeId;

    private String quarter;

    private String avenue;

    private String number;

    private String phoneNumber;

    private String email;

    private String studentComment;

    @NotNull
    private UUID guardianId;

    private UUID photoId;

    @NotNull
    private UUID academicYearId;

    @NotNull
    private UUID classroomId;

    @NotNull
    private LocalDate enrollmentDate;
}
