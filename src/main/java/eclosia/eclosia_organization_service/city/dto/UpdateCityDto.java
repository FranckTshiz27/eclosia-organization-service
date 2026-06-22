package eclosia.eclosia_organization_service.city.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCityDto {

    @NotNull(message = "Province id is required")
    private Long provinceId;

    @NotBlank(message = "Name is required")
    private String name;
}
