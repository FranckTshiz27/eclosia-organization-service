package eclosia.eclosia_organization_service.subject.repository;

import eclosia.eclosia_organization_service.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    boolean existsByCountry_IdAndCode(UUID countryId, String code);

    boolean existsByCountry_IdAndCodeAndIdNot(UUID countryId, String code, UUID id);

    List<Subject> findByCountry_IdOrderByDisplayOrderAsc(UUID countryId);

    List<Subject> findByParentSubject_IdOrderByDisplayOrderAsc(UUID parentSubjectId);
}
