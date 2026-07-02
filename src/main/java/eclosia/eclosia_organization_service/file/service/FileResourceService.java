package eclosia.eclosia_organization_service.file.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.file.entity.FileResource;
import eclosia.eclosia_organization_service.file.repository.FileResourceRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class FileResourceService {

    private final FileResourceRepository repository;

    public FileResource create(FileResource payload) {
        validateFileNameUniqueness(payload.getFileName(), null);

        FileResource fileResource = new FileResource();
        map(fileResource, payload);
        return repository.save(fileResource);
    }

    public List<FileResource> findAll() {
        return repository.findAll();
    }

    public FileResource findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File resource not found"));
    }

    public Resource getContent(UUID id) {
        FileResource fileResource = findById(id);
        Path filePath = Paths.get(fileResource.getPath()).resolve(fileResource.getFileName()).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File content not found");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Unable to read file content", exception);
        }
    }

    public FileResource update(UUID id, FileResource payload) {
        FileResource fileResource = findById(id);
        validateFileNameUniqueness(payload.getFileName(), id);

        map(fileResource, payload);
        return repository.save(fileResource);
    }

    public void delete(UUID id) {
        FileResource fileResource = findById(id);
        repository.delete(fileResource);
    }

    private void validateFileNameUniqueness(String fileName, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByFileName(fileName)) {
                throw new BadRequestException("File name already exists");
            }
            return;
        }

        if (repository.existsByFileNameAndIdNot(fileName, excludeId)) {
            throw new BadRequestException("File name already exists");
        }
    }

    private void map(FileResource target, FileResource payload) {
        target.setFileName(payload.getFileName());
        target.setOriginalName(payload.getOriginalName());
        target.setMimeType(payload.getMimeType());
        target.setSize(payload.getSize());
        target.setPath(payload.getPath());
        target.setExtension(payload.getExtension());
        target.setChecksum(payload.getChecksum());
    }
}
