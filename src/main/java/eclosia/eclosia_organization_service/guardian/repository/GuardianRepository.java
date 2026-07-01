package eclosia.eclosia_organization_service.guardian.repository;

import eclosia.eclosia_organization_service.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    boolean existsBySchool_IdAndFamilyCode(UUID schoolId, String familyCode);

    boolean existsBySchool_IdAndFamilyCodeAndIdNot(UUID schoolId, String familyCode, UUID id);
}
