package eclosia.eclosia_organization_service.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class CreateUserDto {

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must be at most 100 characters")
    private String username;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * true = Keycloak exige UPDATE_PASSWORD (login BFF bloqué jusqu'à
     * POST /auth/complete-temporary-password).
     */
    private Boolean temporaryPassword = false;

    @NotEmpty(message = "At least one role is required")
    private Set<UUID> roleIds;

    /** Optionnel si schoolIds est fourni. */
    private UUID groupId;

    /** Optionnel si groupId est fourni. Au moins une école, toutes du même groupe. */
    private Set<UUID> schoolIds;

    private Boolean active;
}
