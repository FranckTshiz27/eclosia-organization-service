package eclosia.eclosia_organization_service.payment.dto;

import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreatePaymentDto {

    @NotNull(message = "L'inscription est obligatoire")
    private UUID enrollmentId;

    @NotNull(message = "Le frais scolaire est obligatoire")
    private UUID academicFeeId;

    @NotNull(message = "Le taux de change est obligatoire")
    private UUID currencyRateId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;

    @NotNull(message = "Le mode de paiement est obligatoire")
    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String comment;

    @NotNull(message = "La date de paiement est obligatoire")
    private LocalDateTime paymentDate;
}
