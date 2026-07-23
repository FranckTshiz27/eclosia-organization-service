package eclosia.eclosia_organization_service.feature.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.feature.dto.CreateFeatureDto;
import eclosia.eclosia_organization_service.feature.dto.UpdateFeatureDto;
import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.feature.enums.FeatureAction;
import eclosia.eclosia_organization_service.feature.repository.FeatureRepository;
import eclosia.eclosia_organization_service.security_module.entity.SecurityModule;
import eclosia.eclosia_organization_service.security_module.repository.SecurityModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository repository;
    private final SecurityModuleRepository securityModuleRepository;

    @Transactional
    public List<Feature> createAll(List<CreateFeatureDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("At least one feature is required");
        }

        validateBatchDuplicates(dtos);

        List<Feature> created = new ArrayList<>();
        for (CreateFeatureDto dto : dtos) {
            created.add(create(dto));
        }
        created.forEach(this::initializeModule);
        return created;
    }

    private Feature create(CreateFeatureDto dto) {
        validateUniqueness(dto.getModuleId(), dto.getAction(), null);

        Feature feature = new Feature();
        mapFromDto(
                feature,
                dto.getModuleId(),
                dto.getAction(),
                dto.getName(),
                dto.getDescription(),
                dto.getDisplayOrder(),
                dto.getActive(),
                dto.getSystemFeature()
        );
        return repository.save(feature);
    }

    private void validateBatchDuplicates(List<CreateFeatureDto> dtos) {
        Set<String> seen = new HashSet<>();
        for (CreateFeatureDto dto : dtos) {
            String key = dto.getModuleId() + "|" + dto.getAction();
            if (!seen.add(key)) {
                throw new BadRequestException(
                        "Duplicate feature for module and action in request: " + dto.getAction()
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Feature> findAll() {
        List<Feature> features = repository.findAllByOrderByDisplayOrderAscActionAsc();
        features.forEach(this::initializeModule);
        return features;
    }

    @Transactional(readOnly = true)
    public List<Feature> findByModuleId(UUID moduleId) {
        List<Feature> features = repository.findByModule_IdOrderByDisplayOrderAscActionAsc(moduleId);
        features.forEach(this::initializeModule);
        return features;
    }

    @Transactional(readOnly = true)
    public Feature findById(UUID id) {
        Feature feature = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found"));
        initializeModule(feature);
        return feature;
    }

    @Transactional
    public Feature update(UUID id, UpdateFeatureDto dto) {
        Feature feature = findById(id);
        validateUniqueness(dto.getModuleId(), dto.getAction(), id);

        mapFromDto(
                feature,
                dto.getModuleId(),
                dto.getAction(),
                dto.getName(),
                dto.getDescription(),
                dto.getDisplayOrder(),
                dto.getActive(),
                dto.getSystemFeature()
        );
        return repository.save(feature);
    }

    @Transactional
    public void delete(UUID id) {
        Feature feature = findById(id);
        if (Boolean.TRUE.equals(feature.getSystemFeature())) {
            throw new BadRequestException("System features cannot be deleted");
        }
        repository.delete(feature);
    }

    private void validateUniqueness(UUID moduleId, FeatureAction action, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByModule_IdAndAction(moduleId, action)) {
                throw new BadRequestException("Feature already exists for this module and action");
            }
            return;
        }

        if (repository.existsByModule_IdAndActionAndIdNot(moduleId, action, excludeId)) {
            throw new BadRequestException("Feature already exists for this module and action");
        }
    }

    private void mapFromDto(
            Feature feature,
            UUID moduleId,
            FeatureAction action,
            String name,
            String description,
            Integer displayOrder,
            Boolean active,
            Boolean systemFeature
    ) {
        feature.setModule(resolveModule(moduleId));
        feature.setAction(action);
        feature.setName(name);
        feature.setDescription(description);
        feature.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        feature.setActive(active != null ? active : true);
        feature.setSystemFeature(systemFeature != null ? systemFeature : true);
    }

    private SecurityModule resolveModule(UUID moduleId) {
        return securityModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Security module not found"));
    }

    private void initializeModule(Feature feature) {
        if (feature.getModule() != null) {
            feature.getModule().getCode();
        }
    }
}
