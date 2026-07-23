package eclosia.eclosia_organization_service.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteTemporaryPasswordRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Temporary password is required")
    private String temporaryPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}
