package eclosia.eclosia_organization_service.enrollment.service;

import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentFeeResolver {

    private final AcademicFeeRepository academicFeeRepository;
    private final EnrollmentRepository enrollmentRepository;

    public List<AcademicFee> resolveFees(UUID enrollmentId) {
        List<UUID> feeIds = academicFeeRepository.findMatchingFeeIdsByEnrollmentId(enrollmentId);
        logEnrollmentContext(enrollmentId, feeIds);
        if (feeIds.isEmpty()) {
            return List.of();
        }

        List<AcademicFee> fees = academicFeeRepository.findByIdInWithDetails(feeIds);
        fees.forEach(fee -> log.info(
                "Frais retenu - enrollmentId: {}, feeId: {}, code: {}, name: {}, amount: {}, "
                        + "cycleId: {}, levelId: {}, sectionId: {}, optionId: {}, studentCategoryId: {}",
                enrollmentId,
                fee.getId(),
                fee.getCode(),
                fee.getName(),
                fee.getAmount(),
                fee.getAcademicCycleId(),
                fee.getAcademicLevelId(),
                fee.getAcademicSectionId(),
                fee.getAcademicOptionId(),
                fee.getStudentCategoryId()
        ));
        return fees;
    }

    public Optional<AcademicFee> resolveFee(
            UUID enrollmentId,
            UUID feeCategoryId,
            UUID paymentInstallmentId
    ) {
        return resolveFees(enrollmentId).stream()
                .filter(fee -> fee.getFeeCategoryId().equals(feeCategoryId))
                .filter(fee -> Objects.equals(fee.getPaymentInstallmentId(), paymentInstallmentId))
                .findFirst();
    }

    public boolean matches(Enrollment enrollment, AcademicFee fee) {
        List<UUID> matchingFeeIds = academicFeeRepository.findMatchingFeeIdsByEnrollmentId(enrollment.getId());
        boolean matched = matchingFeeIds.contains(fee.getId());
        log.info(
                "Verification frais - enrollmentId: {}, feeId: {}, matchingFeeIds: {}, matched: {}",
                enrollment.getId(),
                fee.getId(),
                matchingFeeIds,
                matched
        );
        return matched;
    }

    private void logEnrollmentContext(UUID enrollmentId, List<UUID> feeIds) {
        enrollmentRepository.findByIdWithPaymentContext(enrollmentId).ifPresentOrElse(
                enrollment -> {
                    Classroom classroom = enrollment.getClassroom();
                    log.info(
                            "Frais correspondants - enrollmentId: {}, enrollmentNumber: {}, "
                                    + "levelId: {}, sectionId: {}, optionId: {}, studentCategoryId: {}, feeIds: {}",
                            enrollmentId,
                            enrollment.getEnrollmentNumber(),
                            classroom != null ? classroom.getAcademicLevelId() : null,
                            classroom != null ? classroom.getAcademicSectionId() : null,
                            classroom != null ? classroom.getAcademicOptionId() : null,
                            enrollment.getStudentCategoryId(),
                            feeIds
                    );
                },
                () -> log.info(
                        "Frais correspondants - enrollmentId: {}, feeIds: {} (inscription introuvable)",
                        enrollmentId,
                        feeIds
                )
        );
    }
}
