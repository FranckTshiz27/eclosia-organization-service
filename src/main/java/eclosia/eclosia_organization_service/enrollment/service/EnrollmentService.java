package eclosia.eclosia_organization_service.enrollment.service;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.city.repository.CityRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.commune.entity.Commune;
import eclosia.eclosia_organization_service.commune.repository.CommuneRepository;
import eclosia.eclosia_organization_service.country.entity.Country;
import eclosia.eclosia_organization_service.country.repository.CountryRepository;
import eclosia.eclosia_organization_service.enrollment.dto.CreateEnrollmentDto;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.file.entity.FileResource;
import eclosia.eclosia_organization_service.file.repository.FileResourceRepository;
import eclosia.eclosia_organization_service.guardian.entity.Guardian;
import eclosia.eclosia_organization_service.guardian.repository.GuardianRepository;
import eclosia.eclosia_organization_service.student.entity.Student;
import eclosia.eclosia_organization_service.student.repository.StudentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Data
public class EnrollmentService {

    private static final int ENROLLMENT_NUMBER_RETRY_LIMIT = 30;
    private static final int STUDENT_NUMBER_RETRY_LIMIT = 30;

    private final EnrollmentRepository repository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final ClassroomRepository classroomRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FileResourceRepository fileResourceRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final CommuneRepository communeRepository;

    @Transactional
    public Enrollment create(CreateEnrollmentDto dto) {
        Guardian guardian = resolveGuardian(dto.getGuardianId());
        Classroom classroom = resolveClassroom(dto.getClassroomId());
        AcademicYear academicYear = resolveAcademicYear(dto.getAcademicYearId());
        FileResource photo = resolvePhoto(dto.getPhotoId());

        validateSchoolConsistency(guardian, classroom, academicYear);

        Student student = buildStudent(dto);
        student.setStudentNumber(generateStudentNumber());
        student = studentRepository.save(student);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentNumber(generateEnrollmentNumber(academicYear));
        enrollment.setEnrollmentDate(dto.getEnrollmentDate());
        enrollment.setStatus("ACTIVE");
        enrollment.setStudent(student);
        enrollment.setGuardian(guardian);
        enrollment.setClassroom(classroom);
        enrollment.setAcademicYear(academicYear);
        enrollment.setPhoto(photo);

        if (repository.existsByStudent_IdAndAcademicYear_Id(student.getId(), academicYear.getId())) {
            throw new BadRequestException("Student is already enrolled for this academic year");
        }

        return repository.save(enrollment);
    }

    public List<Enrollment> findAll(UUID academicYearId, UUID classroomId, UUID guardianId, UUID studentId) {
        if (academicYearId != null) {
            return repository.findByAcademicYear_IdOrderByCreatedAtDesc(academicYearId);
        }
        if (classroomId != null) {
            return repository.findByClassroom_IdOrderByCreatedAtDesc(classroomId);
        }
        if (guardianId != null) {
            return repository.findByGuardian_IdOrderByCreatedAtDesc(guardianId);
        }
        if (studentId != null) {
            return repository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        }
        return repository.findAll();
    }

    public Enrollment findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
    }

    public void delete(UUID id) {
        Enrollment enrollment = findById(id);
        repository.delete(enrollment);
    }

    private Student buildStudent(CreateEnrollmentDto dto) {
        Student student = new Student();
        student.setLastName(dto.getLastName());
        student.setMiddleName(dto.getMiddleName());
        student.setFirstName(dto.getFirstName());
        student.setGender(dto.getGender());
        student.setBirthDate(dto.getBirthDate());
        student.setBirthCountry(resolveCountry(dto.getBirthCountryId(), "Birth country not found"));
        student.setBirthCity(resolveCity(dto.getBirthCityId(), "Birth city not found"));
        student.setBirthCommune(resolveCommune(dto.getBirthCommuneId(), "Birth commune not found"));
        student.setNationality(dto.getNationality());
        student.setCountry(resolveCountry(dto.getCountryId(), "Country not found"));
        student.setCity(resolveCity(dto.getCityId(), "City not found"));
        student.setCommune(resolveCommune(dto.getCommuneId(), "Commune not found"));
        student.setQuarter(dto.getQuarter());
        student.setAvenue(dto.getAvenue());
        student.setNumber(dto.getNumber());
        student.setPhoneNumber(dto.getPhoneNumber());
        student.setEmail(dto.getEmail());
        student.setComment(dto.getStudentComment());
        return student;
    }

    private String generateEnrollmentNumber(AcademicYear academicYear) {
        String yearCode = academicYear.getCode() != null ? academicYear.getCode().replaceAll("\\s+", "") : "AY";
        String prefix = "ENR-" + yearCode + "-";

        for (int attempt = 0; attempt < ENROLLMENT_NUMBER_RETRY_LIMIT; attempt++) {
            String suffix = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            String enrollmentNumber = prefix + suffix;
            if (!repository.existsByEnrollmentNumber(enrollmentNumber)) {
                return enrollmentNumber;
            }
        }

        throw new BadRequestException("Unable to generate unique enrollment number");
    }

    private String generateStudentNumber() {
        for (int attempt = 0; attempt < STUDENT_NUMBER_RETRY_LIMIT; attempt++) {
            String studentNumber = "STD-" + String.format(
                    "%08d",
                    ThreadLocalRandom.current().nextInt(0, 100_000_000)
            );
            if (!studentRepository.existsByStudentNumber(studentNumber)) {
                return studentNumber;
            }
        }
        throw new BadRequestException("Unable to generate unique student number");
    }

    private void validateSchoolConsistency(Guardian guardian, Classroom classroom, AcademicYear academicYear) {
        UUID guardianSchoolId = guardian.getSchoolId();
        UUID classroomSchoolId = classroom.getSchoolId();
        UUID academicYearSchoolId = academicYear.getSchoolId();

        if (!guardianSchoolId.equals(classroomSchoolId) || !guardianSchoolId.equals(academicYearSchoolId)) {
            throw new BadRequestException("Guardian, classroom and academic year must belong to the same school");
        }
    }

    private Guardian resolveGuardian(UUID guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));
    }

    private Classroom resolveClassroom(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found"));
    }

    private AcademicYear resolveAcademicYear(UUID academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private FileResource resolvePhoto(UUID photoId) {
        if (photoId == null) {
            return null;
        }

        return fileResourceRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo file not found"));
    }

    private Country resolveCountry(UUID countryId, String errorMessage) {
        if (countryId == null) {
            return null;
        }

        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    private City resolveCity(UUID cityId, String errorMessage) {
        if (cityId == null) {
            return null;
        }

        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    private Commune resolveCommune(UUID communeId, String errorMessage) {
        if (communeId == null) {
            return null;
        }

        return communeRepository.findById(communeId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }
}
