package eclosia.eclosia_organization_service.classroom_designation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateClassroomDesignationDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private Integer displayOrder;

    private Boolean active;

    private String description;

    @NotNull(message = "School id is required")
    private UUID schoolId;
}
