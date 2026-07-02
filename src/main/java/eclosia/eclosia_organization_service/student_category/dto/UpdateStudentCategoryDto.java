package eclosia.eclosia_organization_service.student_category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateStudentCategoryDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Boolean active;

    @NotNull(message = "School id is required")
    private UUID schoolId;
}
