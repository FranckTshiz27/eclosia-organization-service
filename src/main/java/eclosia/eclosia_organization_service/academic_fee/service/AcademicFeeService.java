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
import eclosia.eclosia_organization_service.fee_category.entity.FeeCategory;
import eclosia.eclosia_organization_service.fee_category.repository.FeeCategoryRepository;
import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import eclosia.eclosia_organization_service.payment_installment.repository.PaymentInstallmentRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicFeeService {

    private final AcademicFeeRepository repository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final AcademicSectionRepository academicSectionRepository;
    private final AcademicOptionRepository academicOptionRepository;
    private final PaymentInstallmentRepository paymentInstallmentRepository;

    public AcademicFee create(CreateAcademicFeeDto dto) {
        ResolvedRelations relations = resolveRelations(dto);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                dto.getCode(),
                null
        );

        AcademicFee academicFee = new AcademicFee();
        mapFromDto(academicFee, dto, relations);
        return repository.save(academicFee);
    }

    public List<AcademicFee> findAll() {
        return repository.findAll();
    }

    public List<AcademicFee> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByCodeAsc(schoolId);
    }

    public List<AcademicFee> findBySchoolIdAndAcademicYearId(UUID schoolId, UUID academicYearId) {
        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to the provided school");
        }
        return repository.findBySchool_IdAndAcademicYear_IdOrderByCodeAsc(schoolId, academicYearId);
    }

    public AcademicFee findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic fee not found"));
    }

    public AcademicFee update(UUID id, UpdateAcademicFeeDto dto) {
        AcademicFee academicFee = findById(id);
        ResolvedRelations relations = resolveRelations(dto);
        validateDuplicate(
                dto.getSchoolId(),
                dto.getAcademicYearId(),
                dto.getAcademicCycleId(),
                dto.getAcademicLevelId(),
                dto.getAcademicSectionId(),
                dto.getAcademicOptionId(),
                dto.getPaymentInstallmentId(),
                dto.getCode(),
                id
        );

        mapFromDto(academicFee, dto, relations);
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
        AcademicSection academicSection = resolveAcademicSection(academicSectionId);
        AcademicOption academicOption = resolveAcademicOption(academicOptionId);
        PaymentInstallment paymentInstallment = resolvePaymentInstallment(paymentInstallmentId);

        validateSchoolConsistency(schoolId, academicYear, feeCategory, paymentInstallment);
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
                paymentInstallment
        );
    }

    private void validateDuplicate(
            UUID schoolId,
            UUID academicYearId,
            UUID academicCycleId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID paymentInstallmentId,
            String code,
            UUID excludeId
    ) {
        if (repository.existsDuplicate(
                schoolId,
                academicYearId,
                academicCycleId,
                academicLevelId,
                academicSectionId,
                academicOptionId,
                paymentInstallmentId,
                code,
                excludeId
        )) {
            throw new BadRequestException("This academic fee already exists");
        }
    }

    private void validateSchoolConsistency(
            UUID schoolId,
            AcademicYear academicYear,
            FeeCategory feeCategory,
            PaymentInstallment paymentInstallment
    ) {
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to the provided school");
        }
        if (!schoolId.equals(feeCategory.getSchoolId())) {
            throw new BadRequestException("Fee category does not belong to the provided school");
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
            CreateAcademicFeeDto dto,
            ResolvedRelations relations
    ) {
        mapFromDto(
                academicFee,
                dto.getCode(),
                dto.getName(),
                dto.getDescription(),
                dto.getAmount(),
                dto.getPayableByInstallment(),
                dto.getActive(),
                dto.getComment(),
                relations
        );
    }

    private void mapFromDto(
            AcademicFee academicFee,
            UpdateAcademicFeeDto dto,
            ResolvedRelations relations
    ) {
        mapFromDto(
                academicFee,
                dto.getCode(),
                dto.getName(),
                dto.getDescription(),
                dto.getAmount(),
                dto.getPayableByInstallment(),
                dto.getActive(),
                dto.getComment(),
                relations
        );
    }

    private void mapFromDto(
            AcademicFee academicFee,
            String code,
            String name,
            String description,
            java.math.BigDecimal amount,
            Boolean payableByInstallment,
            Boolean active,
            String comment,
            ResolvedRelations relations
    ) {
        academicFee.setCode(code);
        academicFee.setName(name);
        academicFee.setDescription(description);
        academicFee.setAmount(amount);
        academicFee.setPayableByInstallment(payableByInstallment != null ? payableByInstallment : false);
        academicFee.setActive(active != null ? active : true);
        academicFee.setComment(comment);
        academicFee.setSchool(relations.school());
        academicFee.setAcademicYear(relations.academicYear());
        academicFee.setFeeCategory(relations.feeCategory());
        academicFee.setAcademicCycle(relations.academicCycle());
        academicFee.setAcademicLevel(relations.academicLevel());
        academicFee.setAcademicSection(relations.academicSection());
        academicFee.setAcademicOption(relations.academicOption());
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

    private record ResolvedRelations(
            School school,
            AcademicYear academicYear,
            FeeCategory feeCategory,
            AcademicCycle academicCycle,
            AcademicLevel academicLevel,
            AcademicSection academicSection,
            AcademicOption academicOption,
            PaymentInstallment paymentInstallment
    ) {
    }
}
