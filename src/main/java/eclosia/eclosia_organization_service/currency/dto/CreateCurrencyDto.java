package eclosia.eclosia_organization_service.currency.dto;

import eclosia.eclosia_organization_service.currency.entity.CurrencySymbolPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCurrencyDto {

    @NotBlank(message = "Code is required")
    @Size(min = 3, max = 3, message = "Code must be exactly 3 characters")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Decimal places is required")
    @Min(value = 0, message = "Decimal places must be greater than or equal to 0")
    @Max(value = 4, message = "Decimal places must be less than or equal to 4")
    private Integer decimalPlaces;

    @Size(max = 3, message = "Numeric code must be at most 3 characters")
    private String numericCode;

    @NotNull(message = "Symbol position is required")
    private CurrencySymbolPosition symbolPosition;

    private Boolean active;

    private String comment;
}
