package eclosia.eclosia_organization_service.teacher_class_assignment.service;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import eclosia.eclosia_organization_service.teacher.repository.TeacherRepository;
import eclosia.eclosia_organization_service.teacher_class_assignment.dto.CreateTeacherClassAssignmentDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.dto.TeacherClassAssignmentResponseDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.dto.UpdateTeacherClassAssignmentDto;
import eclosia.eclosia_organization_service.teacher_class_assignment.entity.TeacherClassAssignment;
import eclosia.eclosia_organization_service.teacher_class_assignment.mapper.TeacherClassAssignmentMapper;
import eclosia.eclosia_organization_service.teacher_class_assignment.repository.TeacherClassAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherClassAssignmentService {

    private final TeacherClassAssignmentRepository repository;
    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherClassAssignmentMapper mapper;

    @Transactional
    public TeacherClassAssignmentResponseDto create(CreateTeacherClassAssignmentDto dto) {
        Teacher teacher = resolveTeacher(dto.getTeacherId());
        School school = resolveSchool(dto.getSchoolId());
        AcademicYear academicYear = resolveOpenAcademicYear(dto.getAcademicYearId());
        Classroom classroom = resolveClassroom(dto.getClassroomId());

        validateSchoolConsistency(teacher, school, classroom);

        if (repository.existsByClassroom_IdAndAcademicYear_IdAndActiveTrue(
                classroom.getId(), academicYear.getId()
        )) {
            throw new BadRequestException(
                    "Classroom already has an active titular for this academic year"
            );
        }

        TeacherClassAssignment assignment = new TeacherClassAssignment();
        assignment.setTeacher(teacher);
        assignment.setSchool(school);
        assignment.setAcademicYear(academicYear);
        assignment.setClassroom(classroom);
        assignment.setActive(true);
        assignment.setRemarks(trimToNull(dto.getRemarks()));

        TeacherClassAssignment saved = repository.save(assignment);
        return mapper.toResponse(requireDetailed(saved.getId()));
    }

    @Transactional
    public TeacherClassAssignmentResponseDto update(UUID id, UpdateTeacherClassAssignmentDto dto) {
        TeacherClassAssignment assignment = requireDetailed(id);
        ensureYearOpen(assignment.getAcademicYear());

        if (dto.getRemarks() != null) {
            assignment.setRemarks(trimToNull(dto.getRemarks()));
        }

        if (dto.getActive() != null) {
            if (Boolean.TRUE.equals(dto.getActive())
                    && !Boolean.TRUE.equals(assignment.getActive())
                    && repository.existsByClassroom_IdAndAcademicYear_IdAndActiveTrueAndIdNot(
                    assignment.getClassroom().getId(),
                    assignment.getAcademicYear().getId(),
                    id
            )) {
                throw new BadRequestException(
                        "Classroom already has an active titular for this academic year"
                );
            }
            assignment.setActive(dto.getActive());
        }

        repository.save(assignment);
        return mapper.toResponse(requireDetailed(id));
    }

    @Transactional
    public TeacherClassAssignmentResponseDto deactivate(UUID id) {
        TeacherClassAssignment assignment = requireDetailed(id);
        ensureYearOpen(assignment.getAcademicYear());
        assignment.setActive(false);
        repository.save(assignment);
        return mapper.toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> findBySchoolAndYear(UUID schoolId, UUID academicYearId) {
        resolveSchool(schoolId);
        resolveAcademicYear(academicYearId);
        return repository.findBySchoolAndYear(schoolId, academicYearId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherClassAssignmentResponseDto findActiveByClassroomAndYear(
            UUID classroomId,
            UUID academicYearId
    ) {
        resolveClassroom(classroomId);
        resolveAcademicYear(academicYearId);
        return repository.findActiveByClassroomAndYear(classroomId, academicYearId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active titular found for this classroom and academic year"
                ));
    }

    @Transactional(readOnly = true)
    public List<TeacherClassAssignmentResponseDto> findByTeacher(UUID teacherId, UUID academicYearId) {
        resolveTeacher(teacherId);
        if (academicYearId != null) {
            resolveAcademicYear(academicYearId);
        }
        return repository.findByTeacher(teacherId, academicYearId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherClassAssignmentResponseDto findById(UUID id) {
        return mapper.toResponse(requireDetailed(id));
    }

    private TeacherClassAssignment requireDetailed(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher class assignment not found"));
    }

    private void validateSchoolConsistency(Teacher teacher, School school, Classroom classroom) {
        if (teacher.getSchool() == null || !Objects.equals(teacher.getSchool().getId(), school.getId())) {
            throw new BadRequestException("Teacher does not belong to the provided school");
        }
        if (classroom.getSchool() == null || !Objects.equals(classroom.getSchool().getId(), school.getId())) {
            throw new BadRequestException("Classroom does not belong to the provided school");
        }
    }

    private Teacher resolveTeacher(UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (teacher.getSchool() != null) {
            teacher.getSchool().getId();
        }
        if (!Boolean.TRUE.equals(teacher.getActive())) {
            throw new BadRequestException("Teacher is inactive");
        }
        return teacher;
    }

    private School resolveSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
    }

    private AcademicYear resolveAcademicYear(UUID academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private AcademicYear resolveOpenAcademicYear(UUID academicYearId) {
        AcademicYear year = resolveAcademicYear(academicYearId);
        ensureYearOpen(year);
        return year;
    }

    private void ensureYearOpen(AcademicYear year) {
        if (!Boolean.TRUE.equals(year.getActive())) {
            throw new BadRequestException("Academic year is closed");
        }
    }

    private Classroom resolveClassroom(UUID classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
        if (classroom.getSchool() != null) {
            classroom.getSchool().getId();
        }
        if (!Boolean.TRUE.equals(classroom.getActive())) {
            throw new BadRequestException("Classroom is inactive");
        }
        return classroom;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
