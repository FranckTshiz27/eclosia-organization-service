package eclosia.eclosia_organization_service.academic_term.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAcademicTermDto {

    @NotNull(message = "Academic year id is required")
    private UUID academicYearId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    private Boolean active;
}
