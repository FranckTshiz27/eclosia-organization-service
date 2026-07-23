package eclosia.eclosia_organization_service.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRoleDto {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private Boolean systemRole;

    private Boolean active;

    private Integer displayOrder;
}
