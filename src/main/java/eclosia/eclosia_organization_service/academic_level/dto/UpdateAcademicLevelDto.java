package eclosia.eclosia_organization_service.academic_level.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAcademicLevelDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Level order is required")
    private Integer levelOrder;

    private Boolean requiresSection;

    private Boolean requiresOption;

    private Boolean active;

    @NotNull(message = "Academic cycle id is required")
    private UUID academicCycleId;
}
