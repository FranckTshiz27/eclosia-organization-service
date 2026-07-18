package eclosia.eclosia_organization_service.academic_year.service;

import eclosia.eclosia_organization_service.academic_year.dto.CreateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.dto.UpdateAcademicYearDto;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class AcademicYearService {

    private final AcademicYearRepository repository;
    private final CountryRepository countryRepository;
    private final SchoolRepository schoolRepository;

    @Transactional
    public AcademicYear create(CreateAcademicYearDto dto) {
        validateCodeUniqueness(dto.getCountryId(), dto.getCode(), null);
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        AcademicYear academicYear = new AcademicYear();
        mapFromDto(
                academicYear,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getActive()
        );
        return repository.save(academicYear);
    }

    public List<AcademicYear> findAll() {
        return repository.findAll();
    }

    public List<AcademicYear> findByCountryId(UUID countryId) {
        return repository.findByCountry_IdOrderByStartDateDesc(countryId);
    }

    public List<AcademicYear> findBySchoolId(UUID schoolId) {
        School school = resolveSchool(schoolId);
        if (school.getCountryId() == null) {
            throw new BadRequestException("School has no country configured");
        }
        return findByCountryId(school.getCountryId());
    }

    public AcademicYear findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    @Transactional
    public AcademicYear update(UUID id, UpdateAcademicYearDto dto) {
        AcademicYear academicYear = findById(id);

        validateCodeUniqueness(dto.getCountryId(), dto.getCode(), id);
        validateDateRange(dto.getStartDate(), dto.getEndDate());

        mapFromDto(
                academicYear,
                dto.getCountryId(),
                dto.getCode(),
                dto.getName(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getActive()
        );
        return repository.save(academicYear);
    }

    public void delete(UUID id) {
        AcademicYear academicYear = findById(id);
        repository.delete(academicYear);
    }

    private void validateCodeUniqueness(UUID countryId, String code, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsByCountry_IdAndCode(countryId, code)) {
                throw new BadRequestException("Academic year code already exists for this country");
            }
            return;
        }

        if (repository.existsByCountry_IdAndCodeAndIdNot(countryId, code, excludeId)) {
            throw new BadRequestException("Academic year code already exists for this country");
        }
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date must be before or equal to end date");
        }
    }

    private void mapFromDto(
            AcademicYear academicYear,
            UUID countryId,
            String code,
            String name,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Boolean active
    ) {
        academicYear.setCountry(resolveCountry(countryId));
        academicYear.setCode(code);
        academicYear.setName(name);
        academicYear.setStartDate(startDate);
        academicYear.setEndDate(endDate);
        academicYear.setActive(active != null ? active : true);
    }

    private Country resolveCountry(UUID countryId) {
        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
