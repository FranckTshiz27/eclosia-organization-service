package eclosia.eclosia_organization_service.academic_curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAcademicCurriculumDto {

    @NotNull(message = "Academic year id is required")
    private UUID academicYearId;

    @NotNull(message = "Academic cycle id is required")
    private UUID academicCycleId;

    @NotNull(message = "Academic level id is required")
    private UUID academicLevelId;

    private UUID academicSectionId;

    private UUID academicOptionId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private Boolean active;
}
