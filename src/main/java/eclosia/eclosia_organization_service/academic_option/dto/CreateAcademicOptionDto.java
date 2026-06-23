package eclosia.eclosia_organization_service.academic_option.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAcademicOptionDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Integer displayOrder;

    private Boolean active;

    @NotNull(message = "Academic section id is required")
    private UUID academicSectionId;
}
