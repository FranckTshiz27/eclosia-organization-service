package eclosia.eclosia_organization_service.file.controller;

import eclosia.eclosia_organization_service.file.entity.FileResource;
import eclosia.eclosia_organization_service.file.service.FileResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "file-resource")
@RequiredArgsConstructor
public class FileResourceController {

    private final FileResourceService service;

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> getContent(@PathVariable UUID id) {
        FileResource fileResource = service.findById(id);
        Resource content = service.getContent(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileResource.getMimeType());
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(content);
    }
}
