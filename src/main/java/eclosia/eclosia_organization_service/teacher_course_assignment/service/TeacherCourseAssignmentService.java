package eclosia.eclosia_organization_service.teacher_course_assignment.service;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject.repository.SubjectRepository;
import eclosia.eclosia_organization_service.teacher.entity.Teacher;
import eclosia.eclosia_organization_service.teacher.repository.TeacherRepository;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.CreateTeacherCourseAssignmentDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.TeacherCourseAssignmentResponseDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.dto.UpdateTeacherCourseAssignmentDto;
import eclosia.eclosia_organization_service.teacher_course_assignment.entity.TeacherCourseAssignment;
import eclosia.eclosia_organization_service.teacher_course_assignment.mapper.TeacherCourseAssignmentMapper;
import eclosia.eclosia_organization_service.teacher_course_assignment.repository.TeacherCourseAssignmentRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherCourseAssignmentService {

    private final TeacherCourseAssignmentRepository repository;
    private final TeacherRepository teacherRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherCourseAssignmentMapper mapper;
    private final Validator validator;

    @Transactional
    public TeacherCourseAssignmentResponseDto create(CreateTeacherCourseAssignmentDto dto) {
        return createAll(List.of(dto)).getFirst();
    }

    @Transactional
    public List<TeacherCourseAssignmentResponseDto> createAll(List<CreateTeacherCourseAssignmentDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new BadRequestException("At least one course assignment is required");
        }

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        for (CreateTeacherCourseAssignmentDto dto : dtos) {
            violations.addAll(validator.validate(dto));
        }
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // Détecte les doublons dans le même payload.
        long distinctKeys = dtos.stream()
                .map(dto -> dto.getTeacherId() + "|" + dto.getClassroomId() + "|"
                        + dto.getSubjectId() + "|" + dto.getAcademicYearId())
                .distinct()
                .count();
        if (distinctKeys != dtos.size()) {
            throw new BadRequestException(
                    "Duplicate course assignment in request (same teacher, classroom, subject and year)"
            );
        }

        return dtos.stream()
                .map(this::createOne)
                .toList();
    }

    private TeacherCourseAssignmentResponseDto createOne(CreateTeacherCourseAssignmentDto dto) {
        Teacher teacher = resolveTeacher(dto.getTeacherId());
        School school = resolveSchool(dto.getSchoolId());
        AcademicYear academicYear = resolveOpenAcademicYear(dto.getAcademicYearId());
        Classroom classroom = resolveClassroom(dto.getClassroomId());
        Subject subject = resolveSubject(dto.getSubjectId());

        validateConsistency(teacher, school, classroom, subject);

        if (repository.existsByTeacher_IdAndClassroom_IdAndSubject_IdAndAcademicYear_IdAndActiveTrue(
                teacher.getId(), classroom.getId(), subject.getId(), academicYear.getId()
        )) {
            throw new BadRequestException(
                    "Active course assignment already exists for this teacher, classroom, subject and year"
            );
        }

        TeacherCourseAssignment assignment = new TeacherCourseAssignment();
        assignment.setTeacher(teacher);
        assignment.setSchool(school);
        assignment.setAcademicYear(academicYear);
        assignment.setClassroom(classroom);
        assignment.setSubject(subject);
        assignment.setWeeklyHours(dto.getWeeklyHours());
        assignment.setCoefficient(dto.getCoefficient());
        assignment.setActive(true);
        assignment.setRemarks(trimToNull(dto.getRemarks()));

        TeacherCourseAssignment saved = repository.save(assignment);
        return mapper.toResponse(requireDetailed(saved.getId()));
    }

    @Transactional
    public TeacherCourseAssignmentResponseDto update(UUID id, UpdateTeacherCourseAssignmentDto dto) {
        TeacherCourseAssignment assignment = requireDetailed(id);
        ensureYearOpen(assignment.getAcademicYear());

        if (dto.getWeeklyHours() != null) {
            assignment.setWeeklyHours(dto.getWeeklyHours());
        }
        if (dto.getCoefficient() != null) {
            assignment.setCoefficient(dto.getCoefficient());
        }
        if (dto.getRemarks() != null) {
            assignment.setRemarks(trimToNull(dto.getRemarks()));
        }
        if (dto.getActive() != null) {
            if (Boolean.TRUE.equals(dto.getActive())
                    && !Boolean.TRUE.equals(assignment.getActive())
                    && repository.existsByTeacher_IdAndClassroom_IdAndSubject_IdAndAcademicYear_IdAndActiveTrueAndIdNot(
                    assignment.getTeacher().getId(),
                    assignment.getClassroom().getId(),
                    assignment.getSubject().getId(),
                    assignment.getAcademicYear().getId(),
                    id
            )) {
                throw new BadRequestException(
                        "Active course assignment already exists for this teacher, classroom, subject and year"
                );
            }
            assignment.setActive(dto.getActive());
        }

        repository.save(assignment);
        return mapper.toResponse(requireDetailed(id));
    }

    @Transactional
    public TeacherCourseAssignmentResponseDto deactivate(UUID id) {
        TeacherCourseAssignment assignment = requireDetailed(id);
        ensureYearOpen(assignment.getAcademicYear());
        assignment.setActive(false);
        repository.save(assignment);
        return mapper.toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public TeacherCourseAssignmentResponseDto findById(UUID id) {
        return mapper.toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseAssignmentResponseDto> findByTeacher(UUID teacherId, UUID academicYearId) {
        resolveTeacher(teacherId);
        if (academicYearId != null) {
            resolveAcademicYear(academicYearId);
        }
        return repository.findByTeacher(teacherId, academicYearId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseAssignmentResponseDto> findByClassroom(UUID classroomId, UUID academicYearId) {
        resolveClassroom(classroomId);
        resolveAcademicYear(academicYearId);
        return repository.findByClassroomAndYear(classroomId, academicYearId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseAssignmentResponseDto> findBySubject(
            UUID subjectId,
            UUID schoolId,
            UUID academicYearId
    ) {
        resolveSubject(subjectId);
        if (schoolId != null) {
            resolveSchool(schoolId);
        }
        if (academicYearId != null) {
            resolveAcademicYear(academicYearId);
        }
        return repository.findBySubject(subjectId, schoolId, academicYearId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseAssignmentResponseDto> findByYearAndSchool(
            UUID academicYearId,
            UUID schoolId
    ) {
        resolveAcademicYear(academicYearId);
        if (schoolId != null) {
            resolveSchool(schoolId);
        }
        return repository.findByYearAndSchool(academicYearId, schoolId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private TeacherCourseAssignment requireDetailed(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher course assignment not found"));
    }

    private void validateConsistency(Teacher teacher, School school, Classroom classroom, Subject subject) {
        if (teacher.getSchool() == null || !Objects.equals(teacher.getSchool().getId(), school.getId())) {
            throw new BadRequestException("Teacher does not belong to the provided school");
        }
        if (classroom.getSchool() == null || !Objects.equals(classroom.getSchool().getId(), school.getId())) {
            throw new BadRequestException("Classroom does not belong to the provided school");
        }
        if (school.getCountryId() != null
                && subject.getCountry() != null
                && !Objects.equals(school.getCountryId(), subject.getCountry().getId())) {
            throw new BadRequestException("Subject does not belong to the school's country");
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

    private Subject resolveSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (subject.getCountry() != null) {
            subject.getCountry().getId();
        }
        if (!Boolean.TRUE.equals(subject.getActive())) {
            throw new BadRequestException("Subject is inactive");
        }
        return subject;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
