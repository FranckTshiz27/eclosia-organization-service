package eclosia.eclosia_organization_service.file.repository;

import eclosia.eclosia_organization_service.file.entity.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileResourceRepository extends JpaRepository<FileResource, UUID> {

    boolean existsByFileName(String fileName);

    boolean existsByFileNameAndIdNot(String fileName, UUID id);
}
