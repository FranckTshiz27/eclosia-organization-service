package eclosia.eclosia_organization_service.school.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.dto.CreateSchoolDto;
import eclosia.eclosia_organization_service.school.dto.UpdateSchoolDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.security.auth.service.CurrentUserService;
import eclosia.eclosia_organization_service.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository repository;
    private final CurrentUserService currentUserService;

    public School create(CreateSchoolDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new BadRequestException("School code already exists");
        }

        School school = new School();
        mapFromCreateDto(school, dto);
        return repository.save(school);
    }

    @Transactional(readOnly = true)
    public List<School> findAll() {
        User currentUser = currentUserService.requireCurrentUser();

        if (currentUserService.isUserAdmin(currentUser)) {
            return repository.findAll();
        }

        Set<UUID> assignedSchoolIds = currentUserService.schoolIds(currentUser);
        if (assignedSchoolIds.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(assignedSchoolIds);
    }

    @Transactional(readOnly = true)
    public School findById(UUID id) {
        School school = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isUserAdmin(currentUser)) {
            return school;
        }

        Set<UUID> assignedSchoolIds = currentUserService.schoolIds(currentUser);
        if (!assignedSchoolIds.contains(id)) {
            throw new ResourceNotFoundException("School not found");
        }
        return school;
    }

    public School update(UUID id, UpdateSchoolDto dto) {
        School school = findById(id);

        if (!school.getCode().equals(dto.getCode()) && repository.existsByCode(dto.getCode())) {
            throw new BadRequestException("School code already exists");
        }

        mapFromUpdateDto(school, dto);
        return repository.save(school);
    }

    public void delete(UUID id) {
        School school = findById(id);
        repository.delete(school);
    }

    private void mapFromCreateDto(School school, CreateSchoolDto dto) {
        school.setCode(dto.getCode());
        school.setName(dto.getName());
        school.setShortName(dto.getShortName());
        school.setDescription(dto.getDescription());
        school.setMotto(dto.getMotto());
        school.setGroupId(dto.getGroupId());
        school.setCountryId(dto.getCountryId());
        school.setStateId(dto.getStateId());
        school.setCityId(dto.getCityId());
        school.setCommuneId(dto.getCommuneId());
        school.setAddress(dto.getAddress());
        school.setLatitude(dto.getLatitude());
        school.setLongitude(dto.getLongitude());
        school.setSchoolType(dto.getSchoolType());
        school.setEmail(dto.getEmail());
        school.setPhone(dto.getPhone());
        school.setAlternatePhone(dto.getAlternatePhone());
        school.setWebsite(dto.getWebsite());
        school.setPrincipalName(dto.getPrincipalName());
        school.setPrincipalPhone(dto.getPrincipalPhone());
        school.setPrincipalEmail(dto.getPrincipalEmail());
        school.setLogo(dto.getLogo());
        school.setCoverImage(dto.getCoverImage());
        school.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 0);
        school.setNumberOfClassrooms(dto.getNumberOfClassrooms() != null ? dto.getNumberOfClassrooms() : 0);
        school.setEstablishmentDate(dto.getEstablishmentDate());
        school.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    private void mapFromUpdateDto(School school, UpdateSchoolDto dto) {
        school.setCode(dto.getCode());
        school.setName(dto.getName());
        school.setShortName(dto.getShortName());
        school.setDescription(dto.getDescription());
        school.setMotto(dto.getMotto());
        school.setGroupId(dto.getGroupId());
        school.setCountryId(dto.getCountryId());
        school.setStateId(dto.getStateId());
        school.setCityId(dto.getCityId());
        school.setCommuneId(dto.getCommuneId());
        school.setAddress(dto.getAddress());
        school.setLatitude(dto.getLatitude());
        school.setLongitude(dto.getLongitude());
        school.setSchoolType(dto.getSchoolType());
        school.setEmail(dto.getEmail());
        school.setPhone(dto.getPhone());
        school.setAlternatePhone(dto.getAlternatePhone());
        school.setWebsite(dto.getWebsite());
        school.setPrincipalName(dto.getPrincipalName());
        school.setPrincipalPhone(dto.getPrincipalPhone());
        school.setPrincipalEmail(dto.getPrincipalEmail());
        school.setLogo(dto.getLogo());
        school.setCoverImage(dto.getCoverImage());
        school.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 0);
        school.setNumberOfClassrooms(dto.getNumberOfClassrooms() != null ? dto.getNumberOfClassrooms() : 0);
        school.setEstablishmentDate(dto.getEstablishmentDate());
        school.setActive(dto.getActive() != null ? dto.getActive() : true);
    }
}
