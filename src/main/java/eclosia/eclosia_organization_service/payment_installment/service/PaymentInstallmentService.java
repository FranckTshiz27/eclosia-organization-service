package eclosia.eclosia_organization_service.payment_installment.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.payment_installment.dto.CreatePaymentInstallmentDto;
import eclosia.eclosia_organization_service.payment_installment.dto.UpdatePaymentInstallmentDto;
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
public class PaymentInstallmentService {

    private final PaymentInstallmentRepository repository;
    private final SchoolRepository schoolRepository;

    public PaymentInstallment create(CreatePaymentInstallmentDto dto) {
        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getDisplayOrder(), null);

        PaymentInstallment paymentInstallment = new PaymentInstallment();
        mapFromDto(
                paymentInstallment,
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getDescription(),
                dto.getActive(),
                dto.getComment(),
                dto.getSchoolId()
        );
        return repository.save(paymentInstallment);
    }

    public List<PaymentInstallment> findAll() {
        return repository.findAll();
    }

    public List<PaymentInstallment> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByDisplayOrderAsc(schoolId);
    }

    public PaymentInstallment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment installment not found"));
    }

    public PaymentInstallment update(UUID id, UpdatePaymentInstallmentDto dto) {
        PaymentInstallment paymentInstallment = findById(id);

        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getDisplayOrder(), id);

        mapFromDto(
                paymentInstallment,
                dto.getCode(),
                dto.getName(),
                dto.getDisplayOrder(),
                dto.getDescription(),
                dto.getActive(),
                dto.getComment(),
                dto.getSchoolId()
        );
        return repository.save(paymentInstallment);
    }

    public void delete(UUID id) {
        PaymentInstallment paymentInstallment = findById(id);
        repository.delete(paymentInstallment);
    }

    private void validateUniqueness(UUID schoolId, String code, Integer displayOrder, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCode(schoolId, code)) {
                throw new BadRequestException("Payment installment code already exists for this school");
            }
            if (repository.existsBySchool_IdAndDisplayOrder(schoolId, displayOrder)) {
                throw new BadRequestException("Payment installment display order already exists for this school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCodeAndIdNot(schoolId, code, excludeId)) {
            throw new BadRequestException("Payment installment code already exists for this school");
        }
        if (repository.existsBySchool_IdAndDisplayOrderAndIdNot(schoolId, displayOrder, excludeId)) {
            throw new BadRequestException("Payment installment display order already exists for this school");
        }
    }

    private void mapFromDto(
            PaymentInstallment paymentInstallment,
            String code,
            String name,
            Integer displayOrder,
            String description,
            Boolean active,
            String comment,
            UUID schoolId
    ) {
        paymentInstallment.setCode(code);
        paymentInstallment.setName(name);
        paymentInstallment.setDisplayOrder(displayOrder);
        paymentInstallment.setDescription(description);
        paymentInstallment.setActive(active != null ? active : true);
        paymentInstallment.setComment(comment);
        paymentInstallment.setSchool(resolveSchool(schoolId));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
