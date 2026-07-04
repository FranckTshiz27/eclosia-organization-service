package eclosia.eclosia_organization_service.currency_rate.dto;

import eclosia.eclosia_organization_service.currency_rate.entity.RateSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateCurrencyRateDto {

    @NotNull(message = "School id is required")
    private UUID schoolId;

    @NotNull(message = "Source currency id is required")
    private UUID sourceCurrencyId;

    @NotNull(message = "Target currency id is required")
    private UUID targetCurrencyId;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Rate must be greater than 0")
    private BigDecimal rate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private Boolean active;

    private RateSource source;

    private String comment;
}
