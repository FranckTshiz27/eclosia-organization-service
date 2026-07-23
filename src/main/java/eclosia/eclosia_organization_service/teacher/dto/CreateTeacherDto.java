package eclosia.eclosia_organization_service.teacher.dto;

import eclosia.eclosia_organization_service.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class CreateTeacherDto {

    // --- Informations personnelles (SecurityUser) ---

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 30)
    private String phone;

    @Size(max = 500)
    private String address;

    /** Optionnel : généré automatiquement si absent. */
    @Size(max = 100)
    private String username;

    /**
     * Mot de passe initial (optionnel).
     * Si absent / vide, un mot de passe temporaire est généré côté serveur
     * et renvoyé une seule fois dans {@code temporaryPassword} de la réponse.
     */
    @Size(min = 8, max = 100)
    private String password;

    // --- Compte / rôles ---

    @NotEmpty(message = "At least one role is required")
    private Set<UUID> roleIds;

    // --- Informations métier (Teacher) ---

    @NotNull(message = "School is required")
    private UUID schoolId;

    @NotBlank(message = "Registration number is required")
    @Size(max = 100)
    private String registrationNumber;

    @NotNull(message = "Hiring date is required")
    private LocalDate hiringDate;

    private LocalDate leavingDate;

    @Size(max = 255)
    private String qualification;

    @Size(max = 255)
    private String specialty;

    @Size(max = 100)
    private String grade;

    /** Signature (data URL base64 ou URL). */
    private String signature;

    private Boolean titular;

    @Size(max = 1000)
    private String remarks;
}
