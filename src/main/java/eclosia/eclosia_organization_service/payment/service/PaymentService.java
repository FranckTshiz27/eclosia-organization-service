package eclosia.eclosia_organization_service.payment.service;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.currency_rate.repository.CurrencyRateRepository;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.payment.dto.CreatePaymentDto;
import eclosia.eclosia_organization_service.payment.dto.UpdatePaymentDto;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
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

    @Transactional
    public Payment create(CreatePaymentDto dto) {
        Enrollment enrollment = resolveEnrollment(dto.getEnrollmentId());
        AcademicFee academicFee = resolveAcademicFee(dto.getAcademicFeeId());
        CurrencyRate currencyRate = resolveCurrencyRate(dto.getCurrencyRateId());

        validateEnrollmentActive(enrollment);
        validateFeeActive(academicFee);
        validateFeeMatchesEnrollment(enrollment, academicFee);
        validateCurrencyRate(enrollment, currencyRate, dto.getCurrencyRateId());

        PaymentStatus status = dto.getStatus() != null ? dto.getStatus() : PaymentStatus.COMPLETED;
        if (status == PaymentStatus.COMPLETED
                && repository.existsByEnrollment_IdAndAcademicFee_IdAndStatus(
                enrollment.getId(),
                academicFee.getId(),
                PaymentStatus.COMPLETED
        )) {
            throw new BusinessException("Ce frais est déjà payé pour cette inscription.");
        }

        Payment payment = new Payment();
        payment.setReceiptNumber(generateReceiptNumber());
        payment.setTransactionReference(generateTransactionReference());
        payment.setEnrollment(enrollment);
        payment.setAcademicFee(academicFee);
        payment.setCurrencyRate(currencyRate);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReferenceNumber(dto.getReferenceNumber());
        payment.setStatus(status);
        payment.setComment(dto.getComment());
        payment.setPaymentDate(dto.getPaymentDate());

        return repository.save(payment);
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
        validateCurrencyRate(payment.getEnrollment(), currencyRate, dto.getCurrencyRateId());

        if (dto.getStatus() == PaymentStatus.COMPLETED
                && payment.getStatus() != PaymentStatus.COMPLETED
                && repository.existsByEnrollment_IdAndAcademicFee_IdAndStatus(
                payment.getEnrollment().getId(),
                payment.getAcademicFee().getId(),
                PaymentStatus.COMPLETED
        )) {
            throw new BusinessException("Ce frais est déjà payé pour cette inscription.");
        }

        payment.setCurrencyRate(currencyRate);
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReferenceNumber(dto.getReferenceNumber());
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

    private AcademicFee resolveAcademicFee(UUID academicFeeId) {
        return academicFeeRepository.findById(academicFeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic fee not found"));
    }

    private CurrencyRate resolveCurrencyRate(UUID currencyRateId) {
        if (currencyRateId == null) {
            return null;
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
        UUID enrollmentYearId = enrollment.getAcademicYear().getId();
        if (!academicFee.getAcademicYear().getId().equals(enrollmentYearId)) {
            throw new BadRequestException("Le frais scolaire n'appartient pas à l'année scolaire de l'inscription");
        }

        UUID enrollmentSchoolId = enrollment.getAcademicYear().getSchoolId();
        if (!academicFee.getSchoolId().equals(enrollmentSchoolId)) {
            throw new BadRequestException("Le frais scolaire n'appartient pas à l'école de l'inscription");
        }

        StudentCategory enrollmentCategory = enrollment.getStudentCategory();
        if (enrollmentCategory == null) {
            throw new BusinessException("L'inscription doit avoir une catégorie d'élève.");
        }
        if (!academicFee.getStudentCategory().getId().equals(enrollmentCategory.getId())) {
            throw new BusinessException("Le frais scolaire ne correspond pas à la catégorie d'élève de l'inscription.");
        }

        Classroom classroom = enrollment.getClassroom();
        if (!academicFee.getAcademicLevel().getId().equals(classroom.getAcademicLevel().getId())) {
            throw new BusinessException("Le frais scolaire ne correspond pas au niveau de la classe de l'inscription.");
        }

        AcademicSection feeSection = academicFee.getAcademicSection();
        AcademicSection classroomSection = classroom.getAcademicSection();
        UUID feeSectionId = feeSection != null ? feeSection.getId() : null;
        UUID classroomSectionId = classroomSection != null ? classroomSection.getId() : null;
        if (feeSectionId == null ? classroomSectionId != null : !feeSectionId.equals(classroomSectionId)) {
            throw new BusinessException("Le frais scolaire ne correspond pas à la section de la classe de l'inscription.");
        }

        AcademicOption feeOption = academicFee.getAcademicOption();
        AcademicOption classroomOption = classroom.getAcademicOption();
        UUID feeOptionId = feeOption != null ? feeOption.getId() : null;
        UUID classroomOptionId = classroomOption != null ? classroomOption.getId() : null;
        if (feeOptionId == null ? classroomOptionId != null : !feeOptionId.equals(classroomOptionId)) {
            throw new BusinessException("Le frais scolaire ne correspond pas à l'option de la classe de l'inscription.");
        }
    }

    private void validateCurrencyRate(Enrollment enrollment, CurrencyRate currencyRate, UUID currencyRateId) {
        if (currencyRateId == null) {
            return;
        }

        UUID schoolId = enrollment.getAcademicYear().getSchoolId();
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
}
