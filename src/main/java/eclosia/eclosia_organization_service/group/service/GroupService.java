package eclosia.eclosia_organization_service.group.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.group.dto.CreateGroupDto;
import eclosia.eclosia_organization_service.group.dto.UpdateGroupDto;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.group.repository.GroupRepository;
import eclosia.eclosia_organization_service.security.auth.service.CurrentUserService;
import eclosia.eclosia_organization_service.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository repository;
    private final CurrentUserService currentUserService;

    public Group create(CreateGroupDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new BadRequestException("Group already exists");
        }

        Group group = new Group();
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setEmail(dto.getEmail());
        group.setPhone(dto.getPhone());

        return repository.save(group);
    }

    @Transactional(readOnly = true)
    public List<Group> findAll() {
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUserService.isUserAdmin(currentUser)) {
            return repository.findAll();
        }

        if (currentUser.getGroup() == null) {
            return List.of();
        }
        return List.of(currentUser.getGroup());
    }

    @Transactional(readOnly = true)
    public Group findById(UUID id) {
        Group group = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isUserAdmin(currentUser)) {
            return group;
        }

        UUID userGroupId = currentUser.groupId();
        if (userGroupId == null || !userGroupId.equals(id)) {
            throw new ResourceNotFoundException("Group not found");
        }
        return group;
    }

    public Group update(UUID id, UpdateGroupDto dto) {
        Group group = findById(id);

        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setEmail(dto.getEmail());
        group.setPhone(dto.getPhone());
        group.setStatus(dto.getStatus());

        return repository.save(group);
    }

    public void delete(UUID id) {
        Group group = findById(id);
        repository.delete(group);
    }
}
