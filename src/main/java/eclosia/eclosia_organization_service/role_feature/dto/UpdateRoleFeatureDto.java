package eclosia.eclosia_organization_service.role_feature.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateRoleFeatureDto {

    @NotNull(message = "Role id is required")
    private UUID roleId;

    @NotNull(message = "Feature id is required")
    private UUID featureId;

    private Boolean active;
}
