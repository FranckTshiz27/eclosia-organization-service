package eclosia.eclosia_organization_service.payment.repository;

import eclosia.eclosia_organization_service.finance.projection.CompletedPaymentProjection;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByReceiptNumber(String receiptNumber);

    boolean existsByTransactionReference(String transactionReference);

    boolean existsByReferenceNumber(String referenceNumber);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.enrollment.id = :enrollmentId
              AND p.academicFee.id = :academicFeeId
              AND p.status = eclosia.eclosia_organization_service.payment.entity.PaymentStatus.COMPLETED
              AND (:excludePaymentId IS NULL OR p.id <> :excludePaymentId)
            """)
    BigDecimal sumCompletedAmountByEnrollmentAndAcademicFee(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("academicFeeId") UUID academicFeeId,
            @Param("excludePaymentId") UUID excludePaymentId
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
            JOIN FETCH e.classroom c
            JOIN FETCH e.academicYear ay
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH p.currencyRate cr
            WHERE c.school.id = :schoolId
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<Payment> findBySchoolIdOrdered(@Param("schoolId") UUID schoolId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH e.student s
            JOIN FETCH e.guardian g
            JOIN FETCH e.academicYear ay
            JOIN FETCH e.classroom c
            JOIN FETCH c.school sch
            JOIN FETCH c.academicLevel al
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH c.academicOption opt
            LEFT JOIN FETCH c.classroomDesignation cd
            LEFT JOIN FETCH e.photo ph
            LEFT JOIN FETCH e.studentCategory stc
            JOIN FETCH p.academicFee f
            LEFT JOIN FETCH f.feeCategory fc
            LEFT JOIN FETCH f.paymentInstallment pi
            JOIN FETCH p.currencyRate cr
            JOIN FETCH cr.sourceCurrency src
            JOIN FETCH cr.targetCurrency tgt
            WHERE p.receiptNumber = :receiptNumber
              AND p.status = eclosia.eclosia_organization_service.payment.entity.PaymentStatus.COMPLETED
            ORDER BY f.name ASC, p.createdAt ASC
            """)
    List<Payment> findByReceiptNumberWithDetails(@Param("receiptNumber") String receiptNumber);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.enrollment e
            JOIN FETCH e.student s
            JOIN FETCH e.academicYear ay
            JOIN FETCH e.classroom c
            JOIN FETCH c.academicLevel al
            JOIN FETCH al.academicCycle classCycle
            LEFT JOIN FETCH c.academicSection sec
            LEFT JOIN FETCH sec.academicCycle classSectionCycle
            LEFT JOIN FETCH c.academicOption opt
            LEFT JOIN FETCH c.classroomDesignation cd
            JOIN FETCH p.academicFee f
            JOIN FETCH f.feeCategory fc
            JOIN FETCH f.academicCycle feeCycle
            LEFT JOIN FETCH f.academicSection feeSection
            LEFT JOIN FETCH feeSection.academicCycle feeSectionCycle
            LEFT JOIN FETCH f.paymentInstallment pi
            LEFT JOIN FETCH p.currencyRate cr
            LEFT JOIN FETCH cr.sourceCurrency src
            LEFT JOIN FETCH cr.targetCurrency tgt
            WHERE c.school.id = :schoolId
              AND ay.id = :academicYearId
            ORDER BY p.paymentDate DESC, p.createdAt ASC
            """)
    List<Payment> findJournalPaymentsWithDetails(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT p.id AS paymentId,
                   p.receiptNumber AS receiptNumber,
                   e.id AS enrollmentId,
                   c.id AS classroomId,
                   f.id AS academicFeeId,
                   fc.id AS feeCategoryId,
                   fc.code AS feeCategoryCode,
                   fc.name AS feeCategoryName,
                   p.amount AS amount,
                   p.paymentMethod AS paymentMethod,
                   p.paymentDate AS paymentDate,
                   p.createdAt AS createdAt,
                   tgt.code AS targetCurrencyCode,
                   src.code AS sourceCurrencyCode,
                   cr.rate AS exchangeRate,
                   s.id AS studentId,
                   s.studentNumber AS studentNumber,
                   s.lastName AS studentLastName,
                   s.firstName AS studentFirstName,
                   s.middleName AS studentMiddleName
            FROM Payment p
            JOIN p.enrollment e
            JOIN e.student s
            JOIN e.academicYear ay
            JOIN e.classroom c
            JOIN p.academicFee f
            JOIN f.feeCategory fc
            LEFT JOIN p.currencyRate cr
            LEFT JOIN cr.targetCurrency tgt
            LEFT JOIN cr.sourceCurrency src
            WHERE c.school.id = :schoolId
              AND ay.id = :academicYearId
              AND p.status = eclosia.eclosia_organization_service.payment.entity.PaymentStatus.COMPLETED
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<CompletedPaymentProjection> findCompletedPaymentsForDashboard(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId
    );

    @Query("""
            SELECT p.id AS paymentId,
                   p.receiptNumber AS receiptNumber,
                   e.id AS enrollmentId,
                   c.id AS classroomId,
                   f.id AS academicFeeId,
                   fc.id AS feeCategoryId,
                   fc.code AS feeCategoryCode,
                   fc.name AS feeCategoryName,
                   p.amount AS amount,
                   p.paymentMethod AS paymentMethod,
                   p.paymentDate AS paymentDate,
                   p.createdAt AS createdAt,
                   tgt.code AS targetCurrencyCode,
                   src.code AS sourceCurrencyCode,
                   cr.rate AS exchangeRate,
                   s.id AS studentId,
                   s.studentNumber AS studentNumber,
                   s.lastName AS studentLastName,
                   s.firstName AS studentFirstName,
                   s.middleName AS studentMiddleName
            FROM Payment p
            JOIN p.enrollment e
            JOIN e.student s
            JOIN e.academicYear ay
            JOIN e.classroom c
            JOIN p.academicFee f
            JOIN f.feeCategory fc
            LEFT JOIN p.currencyRate cr
            LEFT JOIN cr.targetCurrency tgt
            LEFT JOIN cr.sourceCurrency src
            WHERE c.school.id = :schoolId
              AND ay.id = :academicYearId
              AND p.status = eclosia.eclosia_organization_service.payment.entity.PaymentStatus.COMPLETED
            ORDER BY p.paymentDate DESC, p.createdAt DESC
            """)
    List<CompletedPaymentProjection> findRecentCompletedPaymentsForDashboard(
            @Param("schoolId") UUID schoolId,
            @Param("academicYearId") UUID academicYearId,
            Pageable pageable
    );
}
