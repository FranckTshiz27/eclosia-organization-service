package eclosia.eclosia_organization_service.enrollment.service;

import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.city.repository.CityRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.BusinessException;
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
import eclosia.eclosia_organization_service.student_category.entity.StudentCategory;
import eclosia.eclosia_organization_service.student_category.repository.StudentCategoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class EnrollmentService {

    private static final int ENROLLMENT_NUMBER_RETRY_LIMIT = 30;
    private static final int STUDENT_NUMBER_RETRY_LIMIT = 30;
    private static final String ENROLLMENT_UPLOAD_PATH = "uploads/enrollments";

    private final EnrollmentRepository repository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final ClassroomRepository classroomRepository;
    private final AcademicYearRepository academicYearRepository;
    private final FileResourceRepository fileResourceRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final CommuneRepository communeRepository;
    private final StudentCategoryRepository studentCategoryRepository;
    private final EnrollmentFeeResolver enrollmentFeeResolver;

    @Transactional
    public Enrollment create(CreateEnrollmentDto dto, MultipartFile photoFile) {
        Guardian guardian = resolveGuardian(dto.getGuardianId());
        Classroom classroom = resolveClassroom(dto.getClassroomId());
        AcademicYear academicYear = resolveAcademicYear(dto.getAcademicYearId());
        StudentCategory studentCategory = resolveStudentCategory(dto.getStudentCategoryId());
        FileResource photo = storePhoto(photoFile);

        validateSchoolConsistency(guardian, classroom, academicYear, studentCategory);

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
        enrollment.setStudentCategory(studentCategory);
        enrollment.setPhoto(photo);

        if (repository.existsByStudent_IdAndAcademicYear_Id(student.getId(), academicYear.getId())) {
            throw new BadRequestException("Student is already enrolled for this academic year");
        }

        return repository.save(enrollment);
    }

    public Page<Enrollment> findAll(UUID academicYearId, UUID classroomId, UUID guardianId, UUID studentId, int page, int size) {
        Pageable pageable = buildPageable(page, size);

        if (academicYearId != null) {
            return repository.findByAcademicYear_IdOrderByCreatedAtDesc(academicYearId, pageable);
        }
        if (classroomId != null) {
            return repository.findByClassroom_IdOrderByCreatedAtDesc(classroomId, pageable);
        }
        if (guardianId != null) {
            return repository.findByGuardian_IdOrderByCreatedAtDesc(guardianId, pageable);
        }
        if (studentId != null) {
            return repository.findByStudent_IdOrderByCreatedAtDesc(studentId, pageable);
        }
        return repository.findAll(pageable);
    }

    public Page<Enrollment> findByAcademicYearAndSchool(UUID academicYearId, UUID schoolId, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to the provided school");
        }

        Page<Enrollment> enrollments = repository.findByAcademicYear_IdOrderByCreatedAtDesc(academicYearId, pageable);
        log.info(
                "Enrollments fetched - academicYearId: {}, schoolId: {}, page: {}, size: {}, returned: {}",
                academicYearId,
                schoolId,
                page,
                size,
                enrollments.getNumberOfElements()
        );
        return enrollments;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> searchByStudentNameAndAcademicYearAndSchool(
            String name,
            UUID academicYearId,
            UUID schoolId
    ) {
        String trimmedName = name != null ? name.trim() : "";
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("Student name is required for search");
        }

        AcademicYear academicYear = resolveAcademicYear(academicYearId);
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to the provided school");
        }

        List<Enrollment> enrollments = repository.searchByStudentNameAndAcademicYearAndSchool(
                trimmedName,
                academicYearId,
                schoolId
        );
        enrollments.forEach(this::attachAcademicFees);
        log.info(
                "Enrollments searched - name: {}, academicYearId: {}, schoolId: {}, returned: {}",
                trimmedName,
                academicYearId,
                schoolId,
                enrollments.size()
        );
        return enrollments;
    }

    @Transactional(readOnly = true)
    public Enrollment findById(UUID id) {
        Enrollment enrollment = repository.findByIdWithPaymentContext(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        attachAcademicFees(enrollment);
        return enrollment;
    }

    public void delete(UUID id) {
        Enrollment enrollment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
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

    private void validateSchoolConsistency(
            Guardian guardian,
            Classroom classroom,
            AcademicYear academicYear,
            StudentCategory studentCategory
    ) {
        UUID guardianSchoolId = guardian.getSchoolId();
        UUID classroomSchoolId = classroom.getSchoolId();
        UUID academicYearSchoolId = academicYear.getSchoolId();
        UUID studentCategorySchoolId = studentCategory.getSchoolId();

        if (!guardianSchoolId.equals(classroomSchoolId)
                || !guardianSchoolId.equals(academicYearSchoolId)
                || !guardianSchoolId.equals(studentCategorySchoolId)) {
            throw new BadRequestException(
                    "Guardian, classroom, academic year and student category must belong to the same school"
            );
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

    private StudentCategory resolveStudentCategory(UUID studentCategoryId) {
        return studentCategoryRepository.findById(studentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Student category not found"));
    }

    private FileResource storePhoto(MultipartFile photoFile) {
        if (photoFile == null || photoFile.isEmpty()) {
            return null;
        }

        String originalName = extractFileName(photoFile.getOriginalFilename());
        String extension = extractExtension(originalName);
        String generatedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path uploadDirectory = Paths.get(ENROLLMENT_UPLOAD_PATH).toAbsolutePath().normalize();
        Path targetPath = uploadDirectory.resolve(generatedFileName);

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream inputStream = photoFile.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            FileResource fileResource = new FileResource();
            fileResource.setFileName(generatedFileName);
            fileResource.setOriginalName(originalName);
            fileResource.setMimeType(
                    photoFile.getContentType() != null ? photoFile.getContentType() : "application/octet-stream"
            );
            fileResource.setSize(photoFile.getSize());
            fileResource.setPath(uploadDirectory.toString());
            fileResource.setExtension(extension.isBlank() ? null : extension);
            fileResource.setChecksum(calculateSha256(targetPath));

            return fileResourceRepository.save(fileResource);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store photo file", exception);
        }
    }

    private String extractFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-file";
        }
        return Paths.get(originalFilename).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex + 1).toLowerCase();
    }

    private String calculateSha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(filePath);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // Stream file fully to compute digest.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to calculate file checksum", exception);
        }
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

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new BadRequestException("Size must be greater than 0");
        }
        return PageRequest.of(page, size);
    }

    private void attachAcademicFees(Enrollment enrollment) {
        if (enrollment.getStudentCategoryId() == null) {
            throw new BusinessException("L'inscription doit avoir une catégorie d'élève pour récupérer les frais.");
        }

        enrollment.setAcademicFees(enrollmentFeeResolver.resolveFees(enrollment.getId()));
    }
}
