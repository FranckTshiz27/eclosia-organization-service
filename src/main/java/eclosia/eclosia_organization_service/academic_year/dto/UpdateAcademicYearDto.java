package eclosia.eclosia_organization_service.academic_year.dto;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYearStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateAcademicYearDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean current;

    @NotNull(message = "Status is required")
    private AcademicYearStatus status;

    private String description;

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "School academic model id is required")
    private UUID schoolAcademicModelId;
}
