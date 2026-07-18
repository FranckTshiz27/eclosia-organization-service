package eclosia.eclosia_organization_service.academic_fee.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_fee.dto.CreateAcademicFeeDto;
import eclosia.eclosia_organization_service.academic_fee.dto.UpdateAcademicFeeDto;
import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_level.repository.AcademicLevelRepository;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_option.repository.AcademicOptionRepository;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_section.repository.AcademicSectionRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.enrollment.service.EnrollmentFeeResolver;
import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import eclosia.eclosia_organization_service.fee_category.repository.FeeCategoryRepository;
import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import eclosia.eclosia_organization_service.payment_installment.repository.PaymentInstallmentRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
import eclosia.eclosia_organization_service.student_category.repository.StudentCategoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicFeeService {

    private static final Comparator<AcademicFee> FEE_ORDER = Comparator
            .comparing(
                    (AcademicFee fee) -> fee.getAcademicCycle().getDisplayOrder(),
                    Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(fee -> fee.getAcademicCycle().getCode())
            .thenComparing(fee -> fee.getAcademicLevel().getLevelOrder())
            .thenComparing(fee -> fee.getPaymentInstallment() == null
                    ? -1
                    : fee.getPaymentInstallment().getDisplayOrder())
            .thenComparing(fee -> fee.getFeeCategory().getCode())
            .thenComparing(fee -> fee.getStudentCategory().getCode());

    private final AcademicFeeRepository repository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final AcademicSectionRepository academicSectionRepository;
    private final AcademicOptionRepository academicOptionRepository;
    private final PaymentInstallmentRepository paymentInstallmentRepository;
    private final StudentCategoryRepository studentCategoryRepository;
    private final EnrollmentFeeResolver enrollmentFeeResolver;

    @Transactional
    public List<AcademicFee> createAll(List<CreateAcademicFeeDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("At least one academic fee is required");
        }

        validateBatchDuplicates(dtos);

        List<AcademicFee> createdFees = new ArrayList<>();
        for (int index = 0; index < dtos.size(); index++) {
            CreateAcademicFeeDto dto = dtos.get(index);
            if (dto.getStudentCategoryId() == null) {
                throw new BadRequestException(
                        "La catégorie d'élève est obligatoire (élément " + (index + 1) + ")"
                );
            }
            createdFees.add(create(dto));
        }
        return sortFees(createdFees);
    }

    private AcademicFee create(CreateAcademicFeeDto dto) {
        ResolvedRelations relations = resolveRelations(dto);
        String code = generateCode(relations);
        String name = generateName(relations);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getFeeCategoryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getStudentCategoryId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                null
        );

        AcademicFee academicFee = new AcademicFee();
        mapFromDto(
                academicFee,
                code,
                name,
                dto.getAmount(),
                dto.getPayableByInstallment(),
                dto.getActive(),
                relations
        );
        return repository.save(academicFee);
    }

    public List<AcademicFee> findAll() {
        return repository.findAllOrdered();
    }

    public List<AcademicFee> findBySchoolId(UUID schoolId) {
        return repository.findBySchoolIdOrdered(schoolId);
    }

    public List<AcademicFee> findBySchoolIdAndAcademicYearId(UUID schoolId, UUID academicYearId) {
        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        School school = resolveSchool(schoolId);
        AcademicYearCountryValidator.requireSameCountry(school, academicYear);
        return repository.findBySchoolIdAndAcademicYearIdOrdered(schoolId, academicYearId);
    }

    public List<AcademicFee> findByEnrollmentId(UUID enrollmentId) {
        return enrollmentFeeResolver.resolveFees(enrollmentId);
    }

    public AcademicFee findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic fee not found"));
    }

    public AcademicFee update(UUID id, UpdateAcademicFeeDto dto) {
        AcademicFee academicFee = findById(id);
        ResolvedRelations relations = resolveRelations(dto);
        String code = generateCode(relations);
        String name = generateName(relations);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getFeeCategoryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getStudentCategoryId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                id
        );

        mapFromDto(
                academicFee,
                code,
                name,
                dto.getAmount(),
                dto.getPayableByInstallment(),
                dto.getActive(),
                relations
        );
        return repository.save(academicFee);
    }

    public void delete(UUID id) {
        AcademicFee academicFee = findById(id);
        repository.delete(academicFee);
    }

    private ResolvedRelations resolveRelations(CreateAcademicFeeDto dto) {
        return resolveRelations(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getFeeCategoryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getStudentCategoryId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                dto.getPayableByInstallment()
        );
    }

    private ResolvedRelations resolveRelations(UpdateAcademicFeeDto dto) {
        return resolveRelations(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getFeeCategoryId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getStudentCategoryId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                dto.getPayableByInstallment()
        );
    }

    private ResolvedRelations resolveRelations(
            UUID schoolId,
            UUID academicYearId,
            UUID feeCategoryId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID studentCategoryId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID paymentInstallmentId,
            Boolean payableByInstallment
    ) {
        School school = resolveSchool(schoolId);
        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        FeeCategory feeCategory = resolveFeeCategory(feeCategoryId);
        AcademicCycle academicCycle = resolveAcademicCycle(academicCycleId);
        AcademicLevel academicLevel = resolveAcademicLevel(academicLevelId);
        StudentCategory studentCategory = resolveStudentCategory(schoolId, studentCategoryId);
        AcademicSection academicSection = resolveAcademicSection(academicSectionId);
        AcademicOption academicOption = resolveAcademicOption(academicOptionId);
        PaymentInstallment paymentInstallment = resolvePaymentInstallment(paymentInstallmentId);

        validateSchoolConsistency(schoolId, academicYear, feeCategory, studentCategory, paymentInstallment);
        validateAcademicStructure(
                academicCycle,
                academicLevel,
                academicSection,
                academicOption,
                academicSectionId,
                academicOptionId
        );
        validateInstallmentRules(feeCategory, payableByInstallment, paymentInstallment);

        return new ResolvedRelations(
                school,
                academicYear,
                feeCategory,
                academicCycle,
                academicLevel,
                academicSection,
                academicOption,
                studentCategory,
                paymentInstallment
        );
    }

    private void validateBatchDuplicates(List<CreateAcademicFeeDto> dtos) {
        Set<String> keys = new HashSet<>();
        for (CreateAcademicFeeDto dto : dtos) {
            String key = buildDuplicateKey(
                    dto.getSchoolId(),
                    dto.getAcademicYearId(),
                    dto.getFeeCategoryId(),
                    dto.getAcademicCycleId(),
                    dto.getAcademicLevelId(),
                    dto.getStudentCategoryId(),
                    dto.getAcademicSectionId(),
                    dto.getAcademicOptionId(),
                    dto.getPaymentInstallmentId()
            );
            if (!keys.add(key)) {
                throw new BadRequestException("Duplicate academic fee in request");
            }
        }
    }

    private String buildDuplicateKey(
            UUID schoolId,
            UUID academicYearId,
            UUID feeCategoryId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID studentCategoryId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID paymentInstallmentId
    ) {
        return String.join(
                "|",
                String.valueOf(schoolId),
                String.valueOf(academicYearId),
                String.valueOf(feeCategoryId),
                String.valueOf(academicCycleId),
                String.valueOf(academicLevelId),
                String.valueOf(studentCategoryId),
                String.valueOf(academicSectionId),
                String.valueOf(academicOptionId),
                String.valueOf(paymentInstallmentId)
        );
    }

    private String generateCode(ResolvedRelations relations) {
        String prefix = relations.feeCategory().getCode() + "-" + relations.studentCategory().getCode();

        List<String> suffixParts = new ArrayList<>();
        suffixParts.add(relations.academicLevel().getCode());
        if (relations.academicSection() != null) {
            suffixParts.add(relations.academicSection().getCode());
        }
        if (relations.academicOption() != null) {
            suffixParts.add(relations.academicOption().getCode());
        }
        if (relations.paymentInstallment() != null) {
            suffixParts.add(relations.paymentInstallment().getCode());
        }

        String suffix = String.join("-", suffixParts);
        String fullCode = prefix + "-" + suffix;
        if (fullCode.length() <= 20) {
            return fullCode;
        }

        int maxSuffixLength = 20 - prefix.length() - 1;
        if (maxSuffixLength <= 0) {
            return prefix.length() <= 20 ? prefix : prefix.substring(0, 20);
        }
        return prefix + "-" + suffix.substring(0, Math.min(suffix.length(), maxSuffixLength));
    }

    private String generateName(ResolvedRelations relations) {
        StringBuilder name = new StringBuilder(relations.feeCategory().getName());
        name.append(" - ").append(relations.studentCategory().getName());
        name.append(" - ").append(relations.academicLevel().getName());

        if (relations.academicSection() != null) {
            name.append(" - ").append(relations.academicSection().getName());
        }
        if (relations.academicOption() != null) {
            name.append(" - ").append(relations.academicOption().getName());
        }
        if (relations.paymentInstallment() != null) {
            name.append(" - ").append(relations.paymentInstallment().getName());
        }

        String generatedName = name.toString();
        return generatedName.length() <= 150 ? generatedName : generatedName.substring(0, 150);
    }

    private void validateDuplicate(
            UUID schoolId,
            UUID academicYearId,
            UUID feeCategoryId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID studentCategoryId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID paymentInstallmentId,
            UUID excludeId
    ) {
        if (repository.existsDuplicate(
                schoolId,
                academicYearId,
                feeCategoryId,
                academicCycleId,
                academicLevelId,
                studentCategoryId,
                academicSectionId,
                academicOptionId,
                paymentInstallmentId,
                excludeId
        )) {
            throw new BadRequestException("This academic fee already exists");
        }
    }

    private void validateSchoolConsistency(
            UUID schoolId,
            AcademicYear academicYear,
            FeeCategory feeCategory,
            StudentCategory studentCategory,
            PaymentInstallment paymentInstallment
    ) {
        School school = resolveSchool(schoolId);
        AcademicYearCountryValidator.requireSameCountry(school, academicYear);
        if (!schoolId.equals(feeCategory.getSchoolId())) {
            throw new BadRequestException("Fee category does not belong to the provided school");
        }
        if (!schoolId.equals(studentCategory.getSchoolId())) {
            throw new BadRequestException("Student category does not belong to the provided school");
        }
        if (paymentInstallment != null && !schoolId.equals(paymentInstallment.getSchoolId())) {
            throw new BadRequestException("Payment installment does not belong to the provided school");
        }
    }

    private void validateAcademicStructure(
            AcademicCycle academicCycle,
            AcademicLevel academicLevel,
            AcademicSection academicSection,
            AcademicOption academicOption,
            UUID academicSectionId,
            UUID academicOptionId
    ) {
        if (!academicCycle.getId().equals(academicLevel.getAcademicCycle().getId())) {
            throw new BadRequestException("Academic level does not belong to the provided academic cycle");
        }

        validateLevelSectionAndOption(academicLevel, academicSectionId, academicOptionId);

        if (academicSection != null
                && !academicCycle.getId().equals(academicSection.getAcademicCycle().getId())) {
            throw new BadRequestException("Academic section does not belong to the provided academic cycle");
        }

        if (academicOptionId != null && academicSectionId == null) {
            throw new BusinessException("Une section est requise pour associer une option.");
        }

        if (academicOption != null
                && academicSection != null
                && !academicSection.getId().equals(academicOption.getAcademicSection().getId())) {
            throw new BadRequestException("Academic option does not belong to the provided academic section");
        }
    }

    private void validateLevelSectionAndOption(
            AcademicLevel level,
            UUID academicSectionId,
            UUID academicOptionId
    ) {
        if (Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId == null) {
            throw new BusinessException("Une section est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas de section.");
        }

        if (Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId == null) {
            throw new BusinessException("Une option est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas d'option.");
        }
    }

    private void validateInstallmentRules(
            FeeCategory feeCategory,
            Boolean payableByInstallment,
            PaymentInstallment paymentInstallment
    ) {
        boolean payable = payableByInstallment != null ? payableByInstallment : false;

        if (payable && !Boolean.TRUE.equals(feeCategory.getAllowInstallments())) {
            throw new BusinessException("Cette catégorie de frais n'autorise pas le paiement par tranches.");
        }

        if (paymentInstallment != null && !payable) {
            throw new BusinessException("Une tranche de paiement ne peut être associée qu'à un frais payable par tranches.");
        }
    }

    private void mapFromDto(
            AcademicFee academicFee,
            String code,
            String name,
            java.math.BigDecimal amount,
            Boolean payableByInstallment,
            Boolean active,
            ResolvedRelations relations
    ) {
        academicFee.setCode(code);
        academicFee.setName(name);
        academicFee.setAmount(amount);
        academicFee.setPayableByInstallment(payableByInstallment != null ? payableByInstallment : false);
        academicFee.setActive(active != null ? active : true);
        academicFee.setSchool(relations.school());
        academicFee.setAcademicYear(relations.academicYear());
        academicFee.setFeeCategory(relations.feeCategory());
        academicFee.setAcademicCycle(relations.academicCycle());
        academicFee.setAcademicLevel(relations.academicLevel());
        academicFee.setAcademicSection(relations.academicSection());
        academicFee.setAcademicOption(relations.academicOption());
        academicFee.setStudentCategory(relations.studentCategory());
        academicFee.setPaymentInstallment(relations.paymentInstallment());
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private AcademicYear resolveAcademicYear(UUID academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private FeeCategory resolveFeeCategory(UUID feeCategoryId) {
        return feeCategoryRepository.findById(feeCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee category not found"));
    }

    private AcademicCycle resolveAcademicCycle(UUID academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic cycle not found"));
    }

    private AcademicLevel resolveAcademicLevel(UUID academicLevelId) {
        return academicLevelRepository.findById(academicLevelId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic level not found"));
    }

    private AcademicSection resolveAcademicSection(UUID academicSectionId) {
        if (academicSectionId == null) {
            return null;
        }
        return academicSectionRepository.findById(academicSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic section not found"));
    }

    private AcademicOption resolveAcademicOption(UUID academicOptionId) {
        if (academicOptionId == null) {
            return null;
        }
        return academicOptionRepository.findById(academicOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic option not found"));
    }

    private PaymentInstallment resolvePaymentInstallment(UUID paymentInstallmentId) {
        if (paymentInstallmentId == null) {
            return null;
        }
        return paymentInstallmentRepository.findById(paymentInstallmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment installment not found"));
    }

    private StudentCategory resolveStudentCategory(UUID schoolId, UUID studentCategoryId) {
        if (studentCategoryId == null) {
            throw new BadRequestException("La catégorie d'élève est obligatoire");
        }

        StudentCategory studentCategory = studentCategoryRepository.findById(studentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Student category not found"));

        if (!schoolId.equals(studentCategory.getSchoolId())) {
            throw new BadRequestException("Student category does not belong to the provided school");
        }
        if (!Boolean.TRUE.equals(studentCategory.getActive())) {
            throw new BusinessException("Cette catégorie d'élève n'est plus active.");
        }

        return studentCategory;
    }

    private List<AcademicFee> sortFees(List<AcademicFee> fees) {
        return fees.stream().sorted(FEE_ORDER).toList();
    }

    private record ResolvedRelations(
            School school,
            AcademicYear academicYear,
            FeeCategory feeCategory,
            AcademicCycle academicCycle,
            AcademicLevel academicLevel,
            AcademicSection academicSection,
            AcademicOption academicOption,
            StudentCategory studentCategory,
            PaymentInstallment paymentInstallment
    ) {
    }
}
