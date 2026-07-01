package eclosia.eclosia_organization_service.guardian.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.guardian.dto.CreateGuardianDto;
import eclosia.eclosia_organization_service.guardian.dto.UpdateGuardianDto;
import eclosia.eclosia_organization_service.guardian.entity.Gender;
import eclosia.eclosia_organization_service.guardian.entity.Guardian;
import eclosia.eclosia_organization_service.guardian.repository.GuardianRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class GuardianService {

    private final GuardianRepository repository;
    private final SchoolRepository schoolRepository;
    private static final int CREATE_RETRY_LIMIT = 10;

    public Guardian create(CreateGuardianDto dto) {
        validatePhoneUniqueness(dto.getPhoneNumber(), null);
        School school = resolveSchool(dto.getSchoolId());

        for (int attempt = 0; attempt < CREATE_RETRY_LIMIT; attempt++) {
            Guardian guardian = new Guardian();
            guardian.setFamilyCode(generateFamilyCode(dto.getSchoolId()));
            mapFromDto(guardian, dto.getFirstName(), dto.getLastName(), dto.getMiddleName(), dto.getGender(),
                    dto.getPhoneNumber(), dto.getEmail(), dto.getAddress(), dto.getOccupation(),
                    dto.getEmployer(), dto.getActive(), dto.getComment(), school);

            try {
                return repository.save(guardian);
            } catch (DataIntegrityViolationException ex) {
                if (!isFamilyCodeConstraintViolation(ex)) {
                    throw ex;
                }
            }
        }

        throw new BadRequestException("Unable to generate unique family code");
    }

    public List<Guardian> findAll() {
        return repository.findAll();
    }

    public Guardian findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));
    }

    public Guardian update(UUID id, UpdateGuardianDto dto) {
        Guardian guardian = findById(id);
        validatePhoneUniqueness(dto.getPhoneNumber(), id);
        validateFamilyCodeUniqueness(dto.getSchoolId(), guardian.getFamilyCode(), id);
        School school = resolveSchool(dto.getSchoolId());

        mapFromDto(guardian, dto.getFirstName(), dto.getLastName(), dto.getMiddleName(), dto.getGender(),
                dto.getPhoneNumber(), dto.getEmail(), dto.getAddress(), dto.getOccupation(),
                dto.getEmployer(), dto.getActive(), dto.getComment(), school);
        return repository.save(guardian);
    }

    public void delete(UUID id) {
        Guardian guardian = findById(id);
        repository.delete(guardian);
    }

    private void validatePhoneUniqueness(String phoneNumber, UUID excludeId) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        if (excludeId == null) {
            if (repository.existsByPhoneNumber(phoneNumber)) {
                throw new BadRequestException("Phone number already exists");
            }
            return;
        }

        if (repository.existsByPhoneNumberAndIdNot(phoneNumber, excludeId)) {
            throw new BadRequestException("Phone number already exists");
        }
    }

    private void validateFamilyCodeUniqueness(UUID schoolId, String familyCode, UUID excludeId) {
        if (repository.existsBySchool_IdAndFamilyCodeAndIdNot(schoolId, familyCode, excludeId)) {
            throw new BadRequestException("Family code already exists for this school");
        }
    }

    private String generateFamilyCode(UUID schoolId) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String familyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!repository.existsBySchool_IdAndFamilyCode(schoolId, familyCode)) {
                return familyCode;
            }
        }
        throw new BadRequestException("Unable to generate unique family code");
    }

    private boolean isFamilyCodeConstraintViolation(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause == null || rootCause.getMessage() == null) {
            return false;
        }

        return rootCause.getMessage().contains("uk_school_family_code");
    }

    private void mapFromDto(
            Guardian guardian,
            String firstName,
            String lastName,
            String middleName,
            Gender gender,
            String phoneNumber,
            String email,
            String address,
            String occupation,
            String employer,
            Boolean active,
            String comment,
            School school
    ) {
        guardian.setFirstName(firstName);
        guardian.setLastName(lastName);
        guardian.setMiddleName(middleName);
        guardian.setGender(gender);
        guardian.setPhoneNumber(phoneNumber);
        guardian.setEmail(email);
        guardian.setAddress(address);
        guardian.setOccupation(occupation);
        guardian.setEmployer(employer);
        guardian.setActive(active != null ? active : true);
        guardian.setComment(comment);
        guardian.setSchool(school);
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
