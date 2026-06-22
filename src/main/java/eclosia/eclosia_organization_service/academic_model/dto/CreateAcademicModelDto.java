package eclosia.eclosia_organization_service.academic_model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAcademicModelDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Version is required")
    private String version;

    @NotNull(message = "Start year is required")
    private Integer startYear;

    private Integer endYear;

    private Boolean active;

    private UUID countryId;
}
