package eclosia.eclosia_organization_service.academic_curriculum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAcademicCurriculumDto {

    @NotNull(message = "Country id is required")
    private UUID countryId;

    @NotNull(message = "Academic cycle id is required")
    private UUID academicCycleId;

    @NotNull(message = "Academic level id is required")
    private UUID academicLevelId;

    private UUID academicSectionId;

    private UUID academicOptionId;

    private Boolean active;
}
