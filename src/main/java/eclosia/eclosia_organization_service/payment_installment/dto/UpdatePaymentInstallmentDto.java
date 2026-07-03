package eclosia.eclosia_organization_service.payment_installment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdatePaymentInstallmentDto {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    private String description;

    private Boolean active;

    private String comment;

    @NotNull(message = "School id is required")
    private UUID schoolId;
}
