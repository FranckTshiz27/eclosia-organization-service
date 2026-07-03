package eclosia.eclosia_organization_service.payment_installment.repository;

import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentInstallmentRepository extends JpaRepository<PaymentInstallment, UUID> {

    List<PaymentInstallment> findBySchool_IdOrderByDisplayOrderAsc(UUID schoolId);

    boolean existsBySchool_IdAndCode(UUID schoolId, String code);

    boolean existsBySchool_IdAndCodeAndIdNot(UUID schoolId, String code, UUID id);

    boolean existsBySchool_IdAndDisplayOrder(UUID schoolId, Integer displayOrder);

    boolean existsBySchool_IdAndDisplayOrderAndIdNot(UUID schoolId, Integer displayOrder, UUID id);
}
