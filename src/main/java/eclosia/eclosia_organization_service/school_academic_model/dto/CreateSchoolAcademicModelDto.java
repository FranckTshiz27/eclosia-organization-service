package eclosia.eclosia_organization_service.school_academic_model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateSchoolAcademicModelDto {

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Academic model id is required")
    private UUID academicModelId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;

    private String comment;
}
