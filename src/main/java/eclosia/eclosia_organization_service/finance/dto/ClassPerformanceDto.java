package eclosia.eclosia_organization_service.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ClassPerformanceDto {

    private UUID classroomId;
    private String classroomName;
    private long studentCount;
    private BigDecimal expectedAmount;
    private BigDecimal collectedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal recoveryRate;
}
