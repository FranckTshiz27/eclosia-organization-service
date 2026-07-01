package eclosia.eclosia_organization_service.guardian.dto;

import eclosia.eclosia_organization_service.guardian.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateGuardianDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String middleName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private String phoneNumber;

    private String email;

    private String address;

    private String occupation;

    private String employer;

    private Boolean active;

    private String comment;

    @NotNull(message = "School id is required")
    private UUID schoolId;
}
