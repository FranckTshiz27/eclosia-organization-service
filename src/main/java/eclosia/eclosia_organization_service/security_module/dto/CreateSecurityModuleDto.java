package eclosia.eclosia_organization_service.security_module.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSecurityModuleDto {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must be at most 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 100, message = "Icon must be at most 100 characters")
    private String icon;

    private Integer displayOrder;

    private Boolean active;

    private Boolean systemModule;
}
