package eclosia.eclosia_organization_service.finance.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface EnrollmentExpectedProjection {

    UUID getEnrollmentId();

    UUID getClassroomId();

    BigDecimal getExpectedAmount();
}
