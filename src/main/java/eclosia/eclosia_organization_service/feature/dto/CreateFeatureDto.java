package eclosia.eclosia_organization_service.feature.dto;

import eclosia.eclosia_organization_service.feature.enums.FeatureAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateFeatureDto {

    @NotNull(message = "Module id is required")
    private UUID moduleId;

    @NotNull(message = "Action is required")
    private FeatureAction action;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private Integer displayOrder;

    private Boolean active;

    private Boolean systemFeature;
}
