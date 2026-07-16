package eclosia.eclosia_organization_service.academic_period.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAcademicPeriodDto {

    @NotNull(message = "Country id is required")
    private UUID countryId;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Order number is required")
    private Integer orderNumber;

    private Boolean active;
}
