package eclosia.eclosia_organization_service.classroom.service;

import eclosia.eclosia_organization_service.academic_level.entity.AcademicLevel;
import eclosia.eclosia_organization_service.academic_level.repository.AcademicLevelRepository;
import eclosia.eclosia_organization_service.academic_option.entity.AcademicOption;
import eclosia.eclosia_organization_service.academic_option.repository.AcademicOptionRepository;
import eclosia.eclosia_organization_service.academic_section.entity.AcademicSection;
import eclosia.eclosia_organization_service.academic_section.repository.AcademicSectionRepository;
import eclosia.eclosia_organization_service.classroom.dto.CreateClassroomDto;
import eclosia.eclosia_organization_service.classroom.dto.UpdateClassroomDto;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.classroom_designation.entity.ClassroomDesignation;
import eclosia.eclosia_organization_service.classroom_designation.repository.ClassroomDesignationRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
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
public class ClassroomService {

    private final ClassroomRepository repository;
    private final SchoolRepository schoolRepository;
    private final AcademicLevelRepository academicLevelRepository;
    private final AcademicSectionRepository academicSectionRepository;
    private final AcademicOptionRepository academicOptionRepository;
    private final ClassroomDesignationRepository classroomDesignationRepository;
    private final ClassroomNamingService classroomNamingService;

    public Classroom create(CreateClassroomDto dto) {
        AcademicLevel level = resolveAcademicLevel(dto.getAcademicLevelId());
        validateLevelSectionAndOption(level, dto.getAcademicSectionId(), dto.getAcademicOptionId());
        ClassroomDesignation designation = resolveClassroomDesignation(dto.getClassroomDesignationId(), dto.getSchoolId());

        validateDuplicate(dto.getSchoolId(), dto.getAcademicLevelId(), dto.getAcademicSectionId(),
                dto.getAcademicOptionId(), dto.getClassroomDesignationId(), null);

        Classroom classroom = new Classroom();
        mapFromDto(classroom, dto.getCapacity(), dto.getActive(), dto.getDescription(),
                dto.getSchoolId(), level, dto.getAcademicSectionId(), dto.getAcademicOptionId(), designation);
        return enrichDisplayName(repository.save(classroom));
    }

    @Transactional(readOnly = true)
    public List<Classroom> findAll() {
        return repository.findAll().stream()
                .map(this::enrichDisplayName)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Classroom> findBySchoolId(UUID schoolId) {
        return repository.findBySchool_IdOrderByClassroomDesignation_DisplayOrderAsc(schoolId).stream()
                .map(this::enrichDisplayName)
                .toList();
    }

    @Transactional(readOnly = true)
    public Classroom findById(UUID id) {
        Classroom classroom = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        return enrichDisplayName(classroom);
    }

    public Classroom update(UUID id, UpdateClassroomDto dto) {
        Classroom classroom = findById(id);

        AcademicLevel level = resolveAcademicLevel(dto.getAcademicLevelId());
        validateLevelSectionAndOption(level, dto.getAcademicSectionId(), dto.getAcademicOptionId());
        ClassroomDesignation designation = resolveClassroomDesignation(dto.getClassroomDesignationId(), dto.getSchoolId());

        validateDuplicate(dto.getSchoolId(), dto.getAcademicLevelId(), dto.getAcademicSectionId(),
                dto.getAcademicOptionId(), dto.getClassroomDesignationId(), id);

        mapFromDto(classroom, dto.getCapacity(), dto.getActive(), dto.getDescription(),
                dto.getSchoolId(), level, dto.getAcademicSectionId(), dto.getAcademicOptionId(), designation);
        return enrichDisplayName(repository.save(classroom));
    }

    public void delete(UUID id) {
        Classroom classroom = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        repository.delete(classroom);
    }

    private Classroom enrichDisplayName(Classroom classroom) {
        classroom.setDisplayName(classroomNamingService.build(classroom));
        return classroom;
    }

    private void validateDuplicate(
            UUID schoolId,
            UUID academicLevelId,
            UUID academicSectionId,
            UUID academicOptionId,
            UUID classroomDesignationId,
            UUID excludeId
    ) {
        if (repository.existsDuplicate(
                schoolId, academicLevelId, academicSectionId, academicOptionId,
                classroomDesignationId, excludeId)) {
            throw new BadRequestException("This classroom already exists for this school");
        }
    }

    private void validateLevelSectionAndOption(
            AcademicLevel level,
            UUID academicSectionId,
            UUID academicOptionId
    ) {
        if (Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId == null) {
            throw new BusinessException("Une section est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresSection()) && academicSectionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas de section.");
        }

        if (Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId == null) {
            throw new BusinessException("Une option est obligatoire.");
        }

        if (!Boolean.TRUE.equals(level.getRequiresOption()) && academicOptionId != null) {
            throw new BusinessException("Ce niveau n'accepte pas d'option.");
        }
    }

    private void mapFromDto(
            Classroom classroom,
            Integer capacity,
            Boolean active,
            String description,
            UUID schoolId,
            AcademicLevel academicLevel,
            UUID academicSectionId,
            UUID academicOptionId,
            ClassroomDesignation classroomDesignation
    ) {
        classroom.setCapacity(capacity);
        classroom.setActive(active != null ? active : true);
        classroom.setDescription(description);
        classroom.setSchool(resolveSchool(schoolId));
        classroom.setAcademicLevel(academicLevel);
        classroom.setAcademicSection(resolveAcademicSection(academicSectionId));
        classroom.setAcademicOption(resolveAcademicOption(academicOptionId));
        classroom.setClassroomDesignation(classroomDesignation);
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private AcademicLevel resolveAcademicLevel(UUID academicLevelId) {
        return academicLevelRepository.findById(academicLevelId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic level not found"));
    }

    private AcademicSection resolveAcademicSection(UUID academicSectionId) {
        if (academicSectionId == null) {
            return null;
        }
        return academicSectionRepository.findById(academicSectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic section not found"));
    }

    private AcademicOption resolveAcademicOption(UUID academicOptionId) {
        if (academicOptionId == null) {
            return null;
        }
        return academicOptionRepository.findById(academicOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic option not found"));
    }

    private ClassroomDesignation resolveClassroomDesignation(UUID classroomDesignationId, UUID schoolId) {
        ClassroomDesignation designation = classroomDesignationRepository.findById(classroomDesignationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom designation not found"));

        if (!schoolId.equals(designation.getSchoolId())) {
            throw new BusinessException("Cette désignation n'appartient pas à cette école.");
        }

        if (!Boolean.TRUE.equals(designation.getActive())) {
            throw new BusinessException("Cette désignation n'est plus active.");
        }

        return designation;
    }
}
