package eclosia.eclosia_organization_service.school_currency.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateSchoolCurrencyDto {

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Currency id is required")
    private UUID currencyId;

    private Boolean isDefault;

    private Boolean active;

    private String comment;
}
