package eclosia.eclosia_organization_service.subject_sub_domain.repository;

import eclosia.eclosia_organization_service.subject_sub_domain.entity.SubjectSubDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectSubDomainRepository extends JpaRepository<SubjectSubDomain, UUID> {

    boolean existsBySubjectDomain_IdAndCode(UUID subjectDomainId, String code);

    boolean existsBySubjectDomain_IdAndCodeAndIdNot(UUID subjectDomainId, String code, UUID id);

    List<SubjectSubDomain> findBySubjectDomain_IdOrderByDisplayOrderAsc(UUID subjectDomainId);
}
