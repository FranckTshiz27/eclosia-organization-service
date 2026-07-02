package eclosia.eclosia_organization_service.enrollment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateEnrollmentDto {

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Middle name is required")
    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    private String middleName;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotNull(message = "Birth country is required")
    private UUID birthCountryId;

    private UUID birthCityId;

    private UUID birthCommuneId;

    @Size(max = 150, message = "Nationality must not exceed 150 characters")
    private String nationality;

    private UUID countryId;

    private UUID cityId;

    private UUID communeId;

    @Size(max = 150, message = "Quarter must not exceed 150 characters")
    private String quarter;

    @Size(max = 150, message = "Avenue must not exceed 150 characters")
    private String avenue;

    @Size(max = 30, message = "Number must not exceed 30 characters")
    private String number;

    @Pattern(
            regexp = "^$|^[0-9+()\\-\\s]{6,30}$",
            message = "Phone number format is invalid"
    )
    private String phoneNumber;

    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 500, message = "Student comment must not exceed 500 characters")
    private String studentComment;

    @NotNull(message = "Guardian id is required")
    private UUID guardianId;

    @NotNull(message = "Student category id is required")
    private UUID studentCategoryId;

    @NotNull(message = "Academic year id is required")
    private UUID academicYearId;

    @NotNull(message = "Classroom id is required")
    private UUID classroomId;

    @NotNull(message = "Enrollment date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate enrollmentDate;
}
