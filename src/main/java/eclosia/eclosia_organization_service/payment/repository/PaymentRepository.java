package eclosia.eclosia_organization_service.payment.repository;

import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByReceiptNumber(String receiptNumber);

    boolean existsByTransactionReference(String transactionReference);

    boolean existsByEnrollment_IdAndAcademicFee_IdAndStatus(
            UUID enrollmentId,
            UUID academicFeeId,
            PaymentStatus status
    );

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE p.id = :id
            """)
    Optional<Payment> findByIdWithRelations(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findAllOrdered();

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE e.id = :enrollmentId
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findByEnrollmentIdOrdered(@Param("enrollmentId") UUID enrollmentId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE f.id = :academicFeeId
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findByAcademicFeeIdOrdered(@Param("academicFeeId") UUID academicFeeId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE p.status = :status
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findByStatusOrdered(@Param("status") PaymentStatus status);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH e.academicYear ay
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE ay.school.id = :schoolId
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findBySchoolIdOrdered(@Param("schoolId") UUID schoolId);
}
