package eclosia.eclosia_organization_service.payment.service;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.currency_rate.repository.CurrencyRateRepository;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.enrollment.service.EnrollmentFeeResolver;
import eclosia.eclosia_organization_service.payment.dto.CreatePaymentDto;
import eclosia.eclosia_organization_service.payment.dto.UpdatePaymentDto;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Data
public class PaymentService {

    private static final int NUMBER_RETRY_LIMIT = 30;
    private static final DateTimeFormatter TRANSACTION_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository repository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicFeeRepository academicFeeRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final EnrollmentFeeResolver enrollmentFeeResolver;

    @Transactional
    public List<Payment> createAll(List<CreatePaymentDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("At least one payment is required");
        }

        validateBatchPaymentAmounts(dtos);

        Map<UUID, String> receiptNumbersByEnrollment = new HashMap<>();
        List<Payment> createdPayments = new ArrayList<>();
        for (CreatePaymentDto dto : dtos) {
            createdPayments.add(create(dto, receiptNumbersByEnrollment));
        }
        return createdPayments;
    }

    private Payment create(CreatePaymentDto dto, Map<UUID, String> receiptNumbersByEnrollment) {
        Enrollment enrollment = resolveEnrollment(dto.getEnrollmentId());
        AcademicFee academicFee = resolveAcademicFee(enrollment, dto.getAcademicFeeId());
        CurrencyRate currencyRate = resolveCurrencyRate(dto.getCurrencyRateId());

        validateEnrollmentActive(enrollment);
        validateFeeActive(academicFee);
        validateFeeMatchesEnrollment(enrollment, academicFee);
        validateCurrencyRate(enrollment, currencyRate);

        PaymentStatus status = dto.getStatus() != null ? dto.getStatus() : PaymentStatus.COMPLETED;
        validatePaymentAmount(
                enrollment.getId(),
                academicFee,
                dto.getAmount(),
                status,
                null
        );

        Payment payment = new Payment();
        payment.setReceiptNumber(receiptNumbersByEnrollment.computeIfAbsent(
                enrollment.getId(),
                ignored -> generateReceiptNumber()
        ));
        payment.setTransactionReference(generateTransactionReference());
        payment.setEnrollment(enrollment);
        payment.setAcademicFee(academicFee);
        payment.setCurrencyRate(currencyRate);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReferenceNumber(generateReferenceNumber());
        payment.setStatus(status);
        payment.setComment(dto.getComment());
        payment.setPaymentDate(dto.getPaymentDate());

        return repository.saveAndFlush(payment);
    }

    public List<Payment> findAll(UUID enrollmentId, UUID academicFeeId, UUID schoolId, PaymentStatus status) {
        if (enrollmentId != null) {
            return repository.findByEnrollmentIdOrdered(enrollmentId);
        }
        if (academicFeeId != null) {
            return repository.findByAcademicFeeIdOrdered(academicFeeId);
        }
        if (schoolId != null) {
            return repository.findBySchoolIdOrdered(schoolId);
        }
        if (status != null) {
            return repository.findByStatusOrdered(status);
        }
        return repository.findAllOrdered();
    }

    public Payment findById(UUID id) {
        return repository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @Transactional
    public Payment update(UUID id, UpdatePaymentDto dto) {
        Payment payment = findById(id);

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Un paiement annulé ne peut plus être modifié.");
        }

        CurrencyRate currencyRate = resolveCurrencyRate(dto.getCurrencyRateId());
        validateCurrencyRate(payment.getEnrollment(), currencyRate);

        validatePaymentAmount(
                payment.getEnrollment().getId(),
                payment.getAcademicFee(),
                dto.getAmount(),
                dto.getStatus(),
                payment.getId()
        );

        payment.setCurrencyRate(currencyRate);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setStatus(dto.getStatus());
        payment.setComment(dto.getComment());
        payment.setPaymentDate(dto.getPaymentDate());

        return repository.save(payment);
    }

    public void delete(UUID id) {
        Payment payment = findById(id);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException("Un paiement validé ne peut pas être supprimé. Annulez-le d'abord.");
        }
        repository.delete(payment);
    }

    private Enrollment resolveEnrollment(UUID enrollmentId) {
        return enrollmentRepository.findByIdWithPaymentContext(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
    }

    private AcademicFee resolveAcademicFee(Enrollment enrollment, UUID academicFeeId) {
        AcademicFee academicFee = academicFeeRepository.findById(academicFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic fee not found"));

        UUID schoolId = enrollment.getClassroom().getSchoolId();
        if (!schoolId.equals(academicFee.getSchoolId())) {
            throw new BadRequestException("Le frais scolaire n'appartient pas à l'école de l'inscription");
        }
        if (!enrollment.getAcademicYearId().equals(academicFee.getAcademicYearId())) {
            throw new BadRequestException("Le frais scolaire n'appartient pas à l'année scolaire de l'inscription");
        }

        return academicFee;
    }

    private CurrencyRate resolveCurrencyRate(UUID currencyRateId) {
        if (currencyRateId == null) {
            throw new BadRequestException("Le taux de change est obligatoire");
        }
        return currencyRateRepository.findById(currencyRateId)
                .orElseThrow(() -> new ResourceNotFoundException("Currency rate not found"));
    }

    private void validateEnrollmentActive(Enrollment enrollment) {
        if (!"ACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
            throw new BusinessException("Seule une inscription active peut recevoir un paiement.");
        }
    }

    private void validateFeeActive(AcademicFee academicFee) {
        if (!Boolean.TRUE.equals(academicFee.getActive())) {
            throw new BusinessException("Ce frais scolaire n'est plus actif.");
        }
    }

    private void validateFeeMatchesEnrollment(Enrollment enrollment, AcademicFee academicFee) {
        if (!enrollmentFeeResolver.matches(enrollment, academicFee)) {
            throw new BusinessException("Le frais scolaire ne correspond pas à la classe de l'inscription.");
        }
    }

    private void validateCurrencyRate(Enrollment enrollment, CurrencyRate currencyRate) {
        UUID schoolId = enrollment.getClassroom().getSchoolId();
        if (!schoolId.equals(currencyRate.getSchoolId())) {
            throw new BadRequestException("Le taux de change n'appartient pas à l'école de l'inscription");
        }
        if (!Boolean.TRUE.equals(currencyRate.getActive())) {
            throw new BusinessException("Le taux de change sélectionné n'est plus actif.");
        }
    }

    private String generateReceiptNumber() {
        for (int attempt = 0; attempt < NUMBER_RETRY_LIMIT; attempt++) {
            String receiptNumber = "REC-" + String.format(
                    "%010d",
                    ThreadLocalRandom.current().nextLong(0L, 10_000_000_000L)
            );
            if (!repository.existsByReceiptNumber(receiptNumber)) {
                return receiptNumber;
            }
        }
        throw new BadRequestException("Impossible de générer un numéro de reçu unique");
    }

    private String generateTransactionReference() {
        for (int attempt = 0; attempt < NUMBER_RETRY_LIMIT; attempt++) {
            String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            String transactionReference = "TXN-"
                    + TRANSACTION_TIMESTAMP_FORMAT.format(java.time.LocalDateTime.now())
                    + "-"
                    + suffix;
            if (!repository.existsByTransactionReference(transactionReference)) {
                return transactionReference;
            }
        }
        throw new BadRequestException("Impossible de générer une référence de transaction unique");
    }

    private String generateReferenceNumber() {
        for (int attempt = 0; attempt < NUMBER_RETRY_LIMIT; attempt++) {
            String referenceNumber = "REF-" + String.format(
                    "%010d",
                    ThreadLocalRandom.current().nextLong(0L, 10_000_000_000L)
            );
            if (!repository.existsByReferenceNumber(referenceNumber)) {
                return referenceNumber;
            }
        }
        throw new BadRequestException("Impossible de générer une référence unique");
    }

    private void validatePaymentAmount(
            UUID enrollmentId,
            AcademicFee academicFee,
            BigDecimal amount,
            PaymentStatus status,
            UUID excludePaymentId
    ) {
        if (status != PaymentStatus.COMPLETED) {
            return;
        }

        BigDecimal expectedAmount = academicFee.getAmount();
        BigDecimal alreadyPaid = repository.sumCompletedAmountByEnrollmentAndAcademicFee(
                enrollmentId,
                academicFee.getId(),
                excludePaymentId
        );

        if (alreadyPaid.compareTo(expectedAmount) >= 0) {
            throw new BusinessException("Ce frais est déjà entièrement payé pour cette inscription.");
        }

        BigDecimal newTotal = alreadyPaid.add(amount);
        if (newTotal.compareTo(expectedAmount) > 0) {
            throw new BusinessException(
                    "Le montant total des paiements (" + newTotal + ") dépasse le montant attendu ("
                            + expectedAmount + ") pour ce frais."
            );
        }
    }

    private void validateBatchPaymentAmounts(List<CreatePaymentDto> dtos) {
        record PaymentKey(UUID enrollmentId, UUID academicFeeId) {
        }

        var batchTotals = new HashMap<PaymentKey, BigDecimal>();

        for (CreatePaymentDto dto : dtos) {
            PaymentStatus status = dto.getStatus() != null ? dto.getStatus() : PaymentStatus.COMPLETED;
            if (status != PaymentStatus.COMPLETED) {
                continue;
            }

            PaymentKey key = new PaymentKey(dto.getEnrollmentId(), dto.getAcademicFeeId());
            batchTotals.merge(key, dto.getAmount(), BigDecimal::add);
        }

        for (var entry : batchTotals.entrySet()) {
            PaymentKey key = entry.getKey();
            Enrollment enrollment = resolveEnrollment(key.enrollmentId());
            AcademicFee academicFee = resolveAcademicFee(enrollment, key.academicFeeId());
            validatePaymentAmount(
                    key.enrollmentId(),
                    academicFee,
                    entry.getValue(),
                    PaymentStatus.COMPLETED,
                    null
            );
        }
    }
}
