package eclosia.eclosia_organization_service.finance.projection;

import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CompletedPaymentProjection {

    UUID getPaymentId();

    String getReceiptNumber();

    UUID getEnrollmentId();

    UUID getClassroomId();

    UUID getAcademicFeeId();

    UUID getFeeCategoryId();

    String getFeeCategoryCode();

    String getFeeCategoryName();

    BigDecimal getAmount();

    PaymentMethod getPaymentMethod();

    LocalDateTime getPaymentDate();

    LocalDateTime getCreatedAt();

    String getTargetCurrencyCode();

    String getSourceCurrencyCode();

    BigDecimal getExchangeRate();

    UUID getStudentId();

    String getStudentNumber();

    String getStudentLastName();

    String getStudentFirstName();

    String getStudentMiddleName();
}
