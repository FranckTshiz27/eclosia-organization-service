package eclosia.eclosia_organization_service.role_feature.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.feature.repository.FeatureRepository;
import eclosia.eclosia_organization_service.role.entity.Role;
import eclosia.eclosia_organization_service.role.repository.RoleRepository;
import eclosia.eclosia_organization_service.role_feature.dto.CreateRoleFeatureDto;
import eclosia.eclosia_organization_service.role_feature.dto.UpdateRoleFeatureDto;
import eclosia.eclosia_organization_service.role_feature.entity.RoleFeature;
import eclosia.eclosia_organization_service.role_feature.repository.RoleFeatureRepository;
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
public class RoleFeatureService {

    private final RoleFeatureRepository repository;
    private final RoleRepository roleRepository;
    private final FeatureRepository featureRepository;

    @Transactional
    public List<RoleFeature> createAll(List<CreateRoleFeatureDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("At least one role feature is required");
        }

        validateBatchDuplicates(dtos);

        List<RoleFeature> created = new ArrayList<>();
        for (CreateRoleFeatureDto dto : dtos) {
            created.add(create(dto));
        }
        return created;
    }

    private RoleFeature create(CreateRoleFeatureDto dto) {
        validateUniqueness(dto.getRoleId(), dto.getFeatureId(), null);

        RoleFeature roleFeature = new RoleFeature();
        mapFromDto(roleFeature, dto.getRoleId(), dto.getFeatureId(), dto.getActive());
        return repository.save(roleFeature);
    }

    @Transactional(readOnly = true)
    public List<RoleFeature> findAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public List<RoleFeature> findByRoleId(UUID roleId) {
        return repository.findByRole_IdOrderByCreatedAtAsc(roleId);
    }

    @Transactional(readOnly = true)
    public List<RoleFeature> findByFeatureId(UUID featureId) {
        return repository.findByFeature_IdOrderByCreatedAtAsc(featureId);
    }

    @Transactional(readOnly = true)
    public RoleFeature findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role feature not found"));
    }

    @Transactional
    public RoleFeature update(UUID id, UpdateRoleFeatureDto dto) {
        RoleFeature roleFeature = findById(id);
        validateUniqueness(dto.getRoleId(), dto.getFeatureId(), id);

        mapFromDto(roleFeature, dto.getRoleId(), dto.getFeatureId(), dto.getActive());
        return repository.save(roleFeature);
    }

    @Transactional
    public void delete(UUID id) {
        RoleFeature roleFeature = findById(id);
        repository.delete(roleFeature);
    }

    private void validateBatchDuplicates(List<CreateRoleFeatureDto> dtos) {
        Set<String> seen = new HashSet<>();
        for (CreateRoleFeatureDto dto : dtos) {
            String key = dto.getRoleId() + "|" + dto.getFeatureId();
            if (!seen.add(key)) {
                throw new BadRequestException(
                        "Duplicate role-feature pair in request: roleId="
                                + dto.getRoleId()
                                + ", featureId="
                                + dto.getFeatureId()
                );
            }
        }
    }

    private void validateUniqueness(UUID roleId, UUID featureId, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByRole_IdAndFeature_Id(roleId, featureId)) {
                throw new BadRequestException("Role feature already exists for this role and feature");
            }
            return;
        }

        if (repository.existsByRole_IdAndFeature_IdAndIdNot(roleId, featureId, excludeId)) {
            throw new BadRequestException("Role feature already exists for this role and feature");
        }
    }

    private void mapFromDto(
            RoleFeature roleFeature,
            UUID roleId,
            UUID featureId,
            Boolean active
    ) {
        roleFeature.setRole(resolveRole(roleId));
        roleFeature.setFeature(resolveFeature(featureId));
        roleFeature.setActive(active != null ? active : true);
    }

    private Role resolveRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private Feature resolveFeature(UUID featureId) {
        return featureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found"));
    }
}
