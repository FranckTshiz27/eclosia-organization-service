package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecentPaymentDto {

    private UUID paymentId;
    private String receiptNumber;
    private LocalDateTime paymentDate;
    private UUID studentId;
    private String studentNumber;
    private String studentFullName;
    private UUID classroomId;
    private String classroomName;
    private BigDecimal amount;
    private String currencyCode;
    private String paymentMethod;
}
