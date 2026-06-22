package eclosia.eclosia_organization_service.commune.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateCommuneDto {

    @NotNull(message = "City id is required")
    private UUID cityId;

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Long population;
}
