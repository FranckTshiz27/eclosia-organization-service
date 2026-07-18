package eclosia.eclosia_organization_service.subject_domain.repository;

import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectDomainRepository extends JpaRepository<SubjectDomain, UUID> {

    boolean existsByCountry_IdAndCode(UUID countryId, String code);

    boolean existsByCountry_IdAndCodeAndIdNot(UUID countryId, String code, UUID id);

    List<SubjectDomain> findByCountry_IdOrderByDisplayOrderAsc(UUID countryId);
}
