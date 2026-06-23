package eclosia.eclosia_organization_service.academic_section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAcademicSectionDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean active;

    @NotNull(message = "Academic cycle id is required")
    private UUID academicCycleId;
}
