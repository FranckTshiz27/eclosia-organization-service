package eclosia.eclosia_organization_service.group.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eclosia.eclosia_organization_service.group.dto.CreateGroupDto;
import eclosia.eclosia_organization_service.group.dto.UpdateGroupDto;
import eclosia.eclosia_organization_service.group.entity.Group;
import eclosia.eclosia_organization_service.group.repository.GroupRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class GroupService {

    
    private final GroupRepository repository;

    public Group create(CreateGroupDto dto) {

        if (this.repository.existsByName(dto.getName())) {
            throw new RuntimeException("Group already exists");
        }

        Group group = new Group();

        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setEmail(dto.getEmail());
        group.setPhone(dto.getPhone());

        return this.repository.save(group);
    }

    public List<Group> findAll() {
        return repository.findAll();
    }

    public Group findById(UUID id) {

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
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