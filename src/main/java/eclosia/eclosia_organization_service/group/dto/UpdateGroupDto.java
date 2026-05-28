package eclosia.eclosia_organization_service.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGroupDto {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @Email(message = "Invalid email")
    private String email;

    private String phone;

    private String address;

    private Boolean status;
}