package eclosia.eclosia_organization_service.student_category.service;

import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.student_category.dto.CreateStudentCategoryDto;
import eclosia.eclosia_organization_service.student_category.dto.UpdateStudentCategoryDto;
import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
import eclosia.eclosia_organization_service.student_category.repository.StudentCategoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
public class StudentCategoryService {

    private final StudentCategoryRepository repository;
    private final SchoolRepository schoolRepository;

    public StudentCategory create(CreateStudentCategoryDto dto) {
        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), null);

        StudentCategory studentCategory = new StudentCategory();
        mapFromDto(studentCategory, dto.getCode(), dto.getName(), dto.getDescription(), dto.getActive(), dto.getSchoolId());
        return repository.save(studentCategory);
    }

    public List<StudentCategory> findAll() {
        return repository.findAll();
    }

    public List<StudentCategory> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByNameAsc(schoolId);
    }

    public StudentCategory findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student category not found"));
    }

    public StudentCategory update(UUID id, UpdateStudentCategoryDto dto) {
        StudentCategory studentCategory = findById(id);

        validateUniqueness(dto.getSchoolId(), dto.getCode(), dto.getName(), id);

        mapFromDto(studentCategory, dto.getCode(), dto.getName(), dto.getDescription(), dto.getActive(), dto.getSchoolId());
        return repository.save(studentCategory);
    }

    public void delete(UUID id) {
        StudentCategory studentCategory = findById(id);
        repository.delete(studentCategory);
    }

    private void validateUniqueness(UUID schoolId, String code, String name, UUID excludeId) {
        if (excludeId == null) {
            if (repository.existsBySchool_IdAndCode(schoolId, code)) {
                throw new BadRequestException("Student category code already exists for this school");
            }
            if (repository.existsBySchool_IdAndName(schoolId, name)) {
                throw new BadRequestException("Student category name already exists for this school");
            }
            return;
        }

        if (repository.existsBySchool_IdAndCodeAndIdNot(schoolId, code, excludeId)) {
            throw new BadRequestException("Student category code already exists for this school");
        }
        if (repository.existsBySchool_IdAndNameAndIdNot(schoolId, name, excludeId)) {
            throw new BadRequestException("Student category name already exists for this school");
        }
    }

    private void mapFromDto(
            StudentCategory studentCategory,
            String code,
            String name,
            String description,
            Boolean active,
            UUID schoolId
    ) {
        studentCategory.setCode(code);
        studentCategory.setName(name);
        studentCategory.setDescription(description);
        studentCategory.setActive(active != null ? active : true);
        studentCategory.setSchool(resolveSchool(schoolId));
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }
}
