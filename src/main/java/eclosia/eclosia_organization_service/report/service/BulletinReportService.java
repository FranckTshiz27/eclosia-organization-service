package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.academic_curriculum.entity.AcademicCurriculum;
import eclosia.eclosia_organization_service.academic_curriculum.repository.AcademicCurriculumRepository;
import eclosia.eclosia_organization_service.academic_curriculum_subject.entity.AcademicCurriculumSubject;
import eclosia.eclosia_organization_service.academic_curriculum_subject.repository.AcademicCurriculumSubjectRepository;
import eclosia.eclosia_organization_service.academic_period.entity.AcademicPeriod;
import eclosia.eclosia_organization_service.academic_period.enums.AcademicPeriodType;
import eclosia.eclosia_organization_service.academic_period.repository.AcademicPeriodRepository;
import eclosia.eclosia_organization_service.academic_term.entity.AcademicTerm;
import eclosia.eclosia_organization_service.academic_term.repository.AcademicTermRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.report.dto.BulletinPdfResponseDto;
import eclosia.eclosia_organization_service.report.dto.BulletinPreviewResponseDto;
import eclosia.eclosia_organization_service.report.dto.BulletinPrintRequestDto;
import eclosia.eclosia_organization_service.report.dto.BulletinSubjectRowDto;
import eclosia.eclosia_organization_service.report.enums.BulletinPrintMode;
import eclosia.eclosia_organization_service.report.enums.BulletinSortBy;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.student.entity.Student;
import eclosia.eclosia_organization_service.student_grade.entity.StudentGrade;
import eclosia.eclosia_organization_service.student_grade.repository.StudentGradeRepository;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BulletinReportService {

    private static final String REPORT_PATH = "report/bulletin_eleve.jrxml";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ROW_DOMAIN = "DOMAIN_HEADER";
    private static final String ROW_SUBJECT = "SUBJECT";
    private static final String ROW_SUBTOTAL = "SUBTOTAL";
    private static final String ROW_GENERAL = "GENERAL_AVERAGE";

    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicTermRepository academicTermRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AcademicCurriculumRepository academicCurriculumRepository;
    private final AcademicCurriculumSubjectRepository academicCurriculumSubjectRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final ClassroomNamingService classroomNamingService;

    @Transactional(readOnly = true)
    public BulletinPreviewResponseDto preview(BulletinPrintRequestDto request) {
        Context context = prepareContext(request);
        BulletinPreviewResponseDto preview = new BulletinPreviewResponseDto();
        preview.setMode(request.getMode());
        preview.setSchoolId(request.getSchoolId());
        preview.setAcademicYearId(request.getAcademicYearId());
        preview.setTotalStudents(context.enrollments().size());

        Map<UUID, List<Enrollment>> byClassroom = context.enrollments().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClassroom().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        preview.setTotalClassrooms(byClassroom.size());

        List<BulletinPreviewResponseDto.ClassroomPreviewDto> classrooms = new ArrayList<>();
        for (Map.Entry<UUID, List<Enrollment>> entry : byClassroom.entrySet()) {
            BulletinPreviewResponseDto.ClassroomPreviewDto dto =
                    new BulletinPreviewResponseDto.ClassroomPreviewDto();
            dto.setClassroomId(entry.getKey());
            dto.setClassroomName(classroomNamingService.build(entry.getValue().getFirst().getClassroom()));
            dto.setStudentCount(entry.getValue().size());
            classrooms.add(dto);
        }
        preview.setClassrooms(classrooms);
        return preview;
    }

    @Transactional(readOnly = true)
    public BulletinPdfResponseDto generate(BulletinPrintRequestDto request) {
        Context context = prepareContext(request);
        if (context.enrollments().isEmpty()) {
            throw new BadRequestException("No enrollments found for the selected scope");
        }

        List<BulletinSubjectRowDto> rows = buildRows(context, request);
        Map<String, Object> parameters = buildParameters(context, request);

        byte[] pdf;
        try (InputStream reportStream = new ClassPathResource(REPORT_PATH).getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    new JRBeanCollectionDataSource(rows)
            );
            pdf = JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            throw new IllegalStateException("Unable to generate bulletin PDF", exception);
        }

        String fileName = "bulletins-"
                + sanitize(context.school().getCode())
                + "-"
                + sanitize(context.academicYear().getCode())
                + ".pdf";

        return new BulletinPdfResponseDto(
                fileName,
                "application/pdf",
                Base64.getEncoder().encodeToString(pdf)
        );
    }

    private Context prepareContext(BulletinPrintRequestDto request) {
        validateRequest(request);

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        AcademicYearCountryValidator.requireSameCountry(school, academicYear);

        List<Enrollment> enrollments = resolveEnrollments(request, school, academicYear);
        enrollments = sortEnrollments(enrollments, request.getSortBy());

        List<AcademicTerm> terms = academicTermRepository
                .findByAcademicYear_IdOrderByDisplayOrderAsc(academicYear.getId());
        List<AcademicTerm> usedTerms = terms.stream().limit(3).toList();

        List<UUID> termIds = usedTerms.stream().map(AcademicTerm::getId).toList();
        List<AcademicPeriod> allPeriods = termIds.isEmpty()
                ? List.of()
                : academicPeriodRepository.findByAcademicTerm_IdInOrderByDisplayOrderAsc(termIds);

        Map<UUID, List<AcademicPeriod>> periodsByTerm = allPeriods.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getAcademicTerm().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<TermPeriods> termPeriods = new ArrayList<>();
        for (AcademicTerm term : usedTerms) {
            termPeriods.add(mapTermPeriods(term, periodsByTerm.getOrDefault(term.getId(), List.of())));
        }
        while (termPeriods.size() < 3) {
            termPeriods.add(TermPeriods.empty());
        }

        return new Context(school, academicYear, enrollments, termPeriods);
    }

    private void validateRequest(BulletinPrintRequestDto request) {
        if (request.getMode() == null) {
            throw new BadRequestException("Mode is required");
        }
        switch (request.getMode()) {
            case CLASSES -> {
                if (request.getClassroomIds() == null || request.getClassroomIds().isEmpty()) {
                    throw new BadRequestException("classroomIds is required for CLASSES mode");
                }
            }
            case CYCLE -> {
                if (request.getAcademicCycleId() == null) {
                    throw new BadRequestException("academicCycleId is required for CYCLE mode");
                }
            }
            case STUDENT -> {
                if (request.getStudentEnrollmentId() == null) {
                    throw new BadRequestException("studentEnrollmentId is required for STUDENT mode");
                }
            }
            case SCHOOL -> {
            }
        }
    }

    private List<Enrollment> resolveEnrollments(
            BulletinPrintRequestDto request,
            School school,
            AcademicYear academicYear
    ) {
        if (request.getMode() == BulletinPrintMode.STUDENT) {
            Enrollment enrollment = enrollmentRepository.findByIdWithPaymentContext(request.getStudentEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student enrollment not found"));
            if (!school.getId().equals(enrollment.getClassroom().getSchoolId())) {
                throw new BadRequestException("Enrollment does not belong to the selected school");
            }
            if (enrollment.getAcademicYear() == null
                    || !academicYear.getId().equals(enrollment.getAcademicYear().getId())) {
                throw new BadRequestException("Enrollment does not belong to the selected academic year");
            }
            return List.of(enrollment);
        }

        List<Enrollment> enrollments = enrollmentRepository.findForSchoolAndAcademicYearReport(
                academicYear.getId(),
                school.getId()
        );

        return switch (request.getMode()) {
            case SCHOOL -> enrollments;
            case CLASSES -> {
                var classroomIds = request.getClassroomIds().stream().collect(Collectors.toSet());
                yield enrollments.stream()
                        .filter(e -> classroomIds.contains(e.getClassroom().getId()))
                        .toList();
            }
            case CYCLE -> enrollments.stream()
                    .filter(e -> e.getClassroom().getAcademicLevel() != null
                            && e.getClassroom().getAcademicLevel().getAcademicCycle() != null
                            && request.getAcademicCycleId().equals(
                            e.getClassroom().getAcademicLevel().getAcademicCycle().getId()))
                    .toList();
            case STUDENT -> List.of();
        };
    }

    private List<Enrollment> sortEnrollments(List<Enrollment> enrollments, BulletinSortBy sortBy) {
        BulletinSortBy effective = sortBy != null ? sortBy : BulletinSortBy.CLASS_THEN_ALPHABETICAL;
        Comparator<Enrollment> byName = Comparator
                .comparing((Enrollment e) -> nullSafe(e.getStudent().getLastName()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> nullSafe(e.getStudent().getFirstName()), String.CASE_INSENSITIVE_ORDER);
        Comparator<Enrollment> byMatricule = Comparator
                .comparing(
                        (Enrollment e) -> nullSafe(e.getStudent().getStudentNumber()),
                        String.CASE_INSENSITIVE_ORDER
                );
        Comparator<Enrollment> byClass = Comparator
                .comparing(
                        (Enrollment e) -> classroomNamingService.build(e.getClassroom()),
                        String.CASE_INSENSITIVE_ORDER
                );

        return switch (effective) {
            case ALPHABETICAL -> enrollments.stream().sorted(byName).toList();
            case MATRICULE -> enrollments.stream().sorted(byMatricule).toList();
            case CLASS_THEN_ALPHABETICAL -> enrollments.stream().sorted(byClass.thenComparing(byName)).toList();
        };
    }

    private List<BulletinSubjectRowDto> buildRows(Context context, BulletinPrintRequestDto request) {
        List<UUID> enrollmentIds = context.enrollments().stream().map(Enrollment::getId).toList();
        List<StudentGrade> allGrades = enrollmentIds.isEmpty()
                ? List.of()
                : studentGradeRepository.findByStudentEnrollment_IdIn(enrollmentIds);

        Map<UUID, List<StudentGrade>> gradesByEnrollment = allGrades.stream()
                .collect(Collectors.groupingBy(StudentGrade::getStudentEnrollmentId));

        Map<UUID, List<AcademicCurriculumSubject>> subjectsByCurriculum = new HashMap<>();
        Map<String, AcademicCurriculum> curriculumCache = new HashMap<>();

        Map<UUID, BigDecimal> yearTotalsByEnrollment = new HashMap<>();
        Map<UUID, Map<UUID, BigDecimal>> subjectYearTotalsByEnrollment = new HashMap<>();

        for (Enrollment enrollment : context.enrollments()) {
            AcademicCurriculum curriculum = resolveCurriculum(enrollment, context.academicYear(), curriculumCache);
            List<AcademicCurriculumSubject> subjects = curriculum == null
                    ? List.of()
                    : subjectsByCurriculum.computeIfAbsent(
                    curriculum.getId(),
                    academicCurriculumSubjectRepository::findByCurriculumIdWithSubjectAndDomain
            );

            Map<String, BigDecimal> scoreMap = toScoreMap(
                    gradesByEnrollment.getOrDefault(enrollment.getId(), List.of())
            );

            BigDecimal yearTotal = BigDecimal.ZERO;
            Map<UUID, BigDecimal> subjectTotals = new HashMap<>();
            for (AcademicCurriculumSubject acs : subjects) {
                TermScore t1 = computeTermScore(acs, context.termPeriods().get(0), scoreMap);
                TermScore t2 = computeTermScore(acs, context.termPeriods().get(1), scoreMap);
                TermScore t3 = computeTermScore(acs, context.termPeriods().get(2), scoreMap);
                BigDecimal subjectYear = sumNullable(t1.tot(), t2.tot(), t3.tot());
                subjectTotals.put(acs.getId(), subjectYear);
                yearTotal = yearTotal.add(subjectYear);
            }
            yearTotalsByEnrollment.put(enrollment.getId(), yearTotal);
            subjectYearTotalsByEnrollment.put(enrollment.getId(), subjectTotals);
        }

        Map<UUID, String> classRanks = Boolean.TRUE.equals(request.getIncludeStudentRank())
                ? computeClassRanks(context.enrollments(), yearTotalsByEnrollment)
                : Map.of();

        Map<UUID, Map<UUID, BigDecimal>> classAvgByClassroomAndSubject =
                Boolean.TRUE.equals(request.getIncludeClassAverages())
                        ? computeClassAverages(context.enrollments(), subjectYearTotalsByEnrollment)
                        : Map.of();

        List<BulletinSubjectRowDto> rows = new ArrayList<>();
        for (Enrollment enrollment : context.enrollments()) {
            AcademicCurriculum curriculum = resolveCurriculum(enrollment, context.academicYear(), curriculumCache);
            List<AcademicCurriculumSubject> subjects = curriculum == null
                    ? List.of()
                    : subjectsByCurriculum.getOrDefault(curriculum.getId(), List.of());

            Map<String, BigDecimal> scoreMap = toScoreMap(
                    gradesByEnrollment.getOrDefault(enrollment.getId(), List.of())
            );

            StudentIdentity identity = buildIdentity(
                    enrollment,
                    context.school(),
                    context.academicYear(),
                    classRanks.get(enrollment.getId())
            );

            if (subjects.isEmpty()) {
                BulletinSubjectRowDto empty = baseRow(identity);
                empty.setRowType(ROW_SUBJECT);
                empty.setBranchName("(Aucune matière configurée)");
                rows.add(empty);
                continue;
            }

            Map<String, List<AcademicCurriculumSubject>> byDomain = subjects.stream()
                    .sorted(Comparator
                            .comparing((AcademicCurriculumSubject acs) -> resolveDomainDisplayOrder(acs.getSubject()))
                            .thenComparing(acs -> acs.getDisplayOrder() != null ? acs.getDisplayOrder() : Integer.MAX_VALUE))
                    .collect(Collectors.groupingBy(
                            acs -> resolveDomainName(acs.getSubject()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            BigDecimal generalT1 = BigDecimal.ZERO;
            BigDecimal generalT2 = BigDecimal.ZERO;
            BigDecimal generalT3 = BigDecimal.ZERO;
            BigDecimal generalYear = BigDecimal.ZERO;
            BigDecimal generalMax = BigDecimal.ZERO;
            int subjectCount = 0;

            for (Map.Entry<String, List<AcademicCurriculumSubject>> domainEntry : byDomain.entrySet()) {
                boolean hasNamedDomain = domainEntry.getKey() != null
                        && !domainEntry.getKey().isBlank()
                        && !"AUTRES".equalsIgnoreCase(domainEntry.getKey());
                if (hasNamedDomain) {
                    BulletinSubjectRowDto domainHeader = baseRow(identity);
                    domainHeader.setRowType(ROW_DOMAIN);
                    domainHeader.setDomainName(domainEntry.getKey());
                    domainHeader.setBranchName(domainEntry.getKey().toUpperCase());
                    rows.add(domainHeader);
                }

                BigDecimal domainT1 = BigDecimal.ZERO;
                BigDecimal domainT2 = BigDecimal.ZERO;
                BigDecimal domainT3 = BigDecimal.ZERO;
                BigDecimal domainYear = BigDecimal.ZERO;
                BigDecimal domainMax = BigDecimal.ZERO;

                for (AcademicCurriculumSubject acs : domainEntry.getValue()) {
                    TermScore t1 = computeTermScore(acs, context.termPeriods().get(0), scoreMap);
                    TermScore t2 = computeTermScore(acs, context.termPeriods().get(1), scoreMap);
                    TermScore t3 = computeTermScore(acs, context.termPeriods().get(2), scoreMap);
                    BigDecimal yearTot = sumNullable(t1.tot(), t2.tot(), t3.tot());
                    BigDecimal maxPoints = acs.getMaximumPoints() != null ? acs.getMaximumPoints() : BigDecimal.ZERO;

                    BulletinSubjectRowDto row = baseRow(identity);
                    row.setRowType(ROW_SUBJECT);
                    row.setDomainName(domainEntry.getKey());
                    row.setBranchName(acs.getSubject().getName() != null
                            ? acs.getSubject().getName().toUpperCase()
                            : "");
                    row.setMaxPoints(formatScore(maxPoints));
                    fillTermColumns(row, 1, t1);
                    fillTermColumns(row, 2, t2);
                    fillTermColumns(row, 3, t3);
                    row.setYearMax(formatScore(maxPoints.multiply(BigDecimal.valueOf(3))));
                    row.setYearTot(formatScore(yearTot));
                    if (Boolean.TRUE.equals(request.getIncludeClassAverages())) {
                        BigDecimal avg = classAvgByClassroomAndSubject
                                .getOrDefault(enrollment.getClassroom().getId(), Map.of())
                                .get(acs.getId());
                        row.setClassAvg(formatScore(avg));
                    }
                    rows.add(row);

                    domainT1 = domainT1.add(nullToZero(t1.tot()));
                    domainT2 = domainT2.add(nullToZero(t2.tot()));
                    domainT3 = domainT3.add(nullToZero(t3.tot()));
                    domainYear = domainYear.add(yearTot);
                    domainMax = domainMax.add(maxPoints);

                    generalT1 = generalT1.add(nullToZero(t1.tot()));
                    generalT2 = generalT2.add(nullToZero(t2.tot()));
                    generalT3 = generalT3.add(nullToZero(t3.tot()));
                    generalYear = generalYear.add(yearTot);
                    generalMax = generalMax.add(maxPoints);
                    subjectCount++;
                }

                BulletinSubjectRowDto subtotal = baseRow(identity);
                subtotal.setRowType(ROW_SUBTOTAL);
                subtotal.setDomainName(domainEntry.getKey());
                subtotal.setBranchName("SOUS-TOTAL");
                subtotal.setT1Tot(formatScore(domainT1));
                subtotal.setT2Tot(formatScore(domainT2));
                subtotal.setT3Tot(formatScore(domainT3));
                subtotal.setYearMax(formatScore(domainMax.multiply(BigDecimal.valueOf(3))));
                subtotal.setYearTot(formatScore(domainYear));
                rows.add(subtotal);
            }

            BulletinSubjectRowDto general = baseRow(identity);
            general.setRowType(ROW_GENERAL);
            general.setBranchName("MOY. GENERALE");
            general.setT1Tot(formatScore(generalT1));
            general.setT2Tot(formatScore(generalT2));
            general.setT3Tot(formatScore(generalT3));
            general.setYearMax(formatScore(generalMax.multiply(BigDecimal.valueOf(3))));
            general.setYearTot(formatScore(generalYear));
            if (subjectCount > 0) {
                BigDecimal divisor = BigDecimal.valueOf(subjectCount);
                general.setT1Moy(formatScore(generalT1.divide(divisor, 2, RoundingMode.HALF_UP)));
                general.setT2Moy(formatScore(generalT2.divide(divisor, 2, RoundingMode.HALF_UP)));
                general.setT3Moy(formatScore(generalT3.divide(divisor, 2, RoundingMode.HALF_UP)));
            }
            rows.add(general);
        }

        return rows;
    }

    private Map<UUID, String> computeClassRanks(
            List<Enrollment> enrollments,
            Map<UUID, BigDecimal> yearTotalsByEnrollment
    ) {
        Map<UUID, List<Enrollment>> byClassroom = enrollments.stream()
                .collect(Collectors.groupingBy(e -> e.getClassroom().getId()));

        Map<UUID, String> ranks = new HashMap<>();
        for (List<Enrollment> classmates : byClassroom.values()) {
            List<Enrollment> ordered = classmates.stream()
                    .sorted(Comparator.comparing(
                            (Enrollment e) -> yearTotalsByEnrollment.getOrDefault(e.getId(), BigDecimal.ZERO)
                    ).reversed())
                    .toList();
            int position = 1;
            for (Enrollment enrollment : ordered) {
                ranks.put(enrollment.getId(), position + " / " + ordered.size());
                position++;
            }
        }
        return ranks;
    }

    private Map<UUID, Map<UUID, BigDecimal>> computeClassAverages(
            List<Enrollment> enrollments,
            Map<UUID, Map<UUID, BigDecimal>> subjectYearTotalsByEnrollment
    ) {
        Map<UUID, Map<UUID, List<BigDecimal>>> accumulator = new HashMap<>();
        for (Enrollment enrollment : enrollments) {
            UUID classroomId = enrollment.getClassroom().getId();
            Map<UUID, BigDecimal> subjectTotals =
                    subjectYearTotalsByEnrollment.getOrDefault(enrollment.getId(), Map.of());
            Map<UUID, List<BigDecimal>> classroomMap =
                    accumulator.computeIfAbsent(classroomId, id -> new HashMap<>());
            for (Map.Entry<UUID, BigDecimal> entry : subjectTotals.entrySet()) {
                classroomMap.computeIfAbsent(entry.getKey(), id -> new ArrayList<>()).add(entry.getValue());
            }
        }

        Map<UUID, Map<UUID, BigDecimal>> averages = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, List<BigDecimal>>> classroomEntry : accumulator.entrySet()) {
            Map<UUID, BigDecimal> subjectAvgs = new HashMap<>();
            for (Map.Entry<UUID, List<BigDecimal>> subjectEntry : classroomEntry.getValue().entrySet()) {
                List<BigDecimal> values = subjectEntry.getValue();
                if (values.isEmpty()) {
                    continue;
                }
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                subjectAvgs.put(
                        subjectEntry.getKey(),
                        sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP)
                );
            }
            averages.put(classroomEntry.getKey(), subjectAvgs);
        }
        return averages;
    }

    private AcademicCurriculum resolveCurriculum(
            Enrollment enrollment,
            AcademicYear academicYear,
            Map<String, AcademicCurriculum> cache
    ) {
        Classroom classroom = enrollment.getClassroom();
        if (classroom.getAcademicLevel() == null || classroom.getAcademicLevel().getAcademicCycle() == null) {
            return null;
        }
        UUID sectionId = classroom.getAcademicSection() != null ? classroom.getAcademicSection().getId() : null;
        UUID optionId = classroom.getAcademicOption() != null ? classroom.getAcademicOption().getId() : null;
        String key = academicYear.getId()
                + "|" + classroom.getAcademicLevel().getAcademicCycle().getId()
                + "|" + classroom.getAcademicLevel().getId()
                + "|" + sectionId
                + "|" + optionId;

        return cache.computeIfAbsent(key, ignored ->
                academicCurriculumRepository.findByCurriculumKeys(
                        academicYear.getId(),
                        classroom.getAcademicLevel().getAcademicCycle().getId(),
                        classroom.getAcademicLevel().getId(),
                        sectionId,
                        optionId
                ).orElse(null)
        );
    }

    private Map<String, BigDecimal> toScoreMap(List<StudentGrade> grades) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (StudentGrade grade : grades) {
            if (grade.getAcademicPeriod() == null || grade.getAcademicCurriculumSubject() == null) {
                continue;
            }
            map.put(
                    grade.getAcademicPeriod().getId() + "|" + grade.getAcademicCurriculumSubject().getId(),
                    grade.getScore()
            );
        }
        return map;
    }

    private TermPeriods mapTermPeriods(AcademicTerm term, List<AcademicPeriod> periods) {
        List<AcademicPeriod> periodSlots = periods.stream()
                .filter(p -> p.getPeriodType() == AcademicPeriodType.PERIOD)
                .sorted(Comparator.comparing(AcademicPeriod::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        AcademicPeriod p1 = periodSlots.isEmpty() ? null : periodSlots.getFirst();
        AcademicPeriod p2 = periodSlots.size() > 1 ? periodSlots.get(1) : null;
        AcademicPeriod exam = periods.stream()
                .filter(p -> p.getPeriodType() == AcademicPeriodType.EXAM)
                .sorted(Comparator.comparing(AcademicPeriod::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .findFirst()
                .orElse(null);
        return new TermPeriods(term, p1, p2, exam);
    }

    private TermScore computeTermScore(
            AcademicCurriculumSubject acs,
            TermPeriods termPeriods,
            Map<String, BigDecimal> scoreMap
    ) {
        if (termPeriods == null || termPeriods.term() == null) {
            return TermScore.empty(acs.getMaximumPoints());
        }

        BigDecimal p1 = scoreOf(scoreMap, termPeriods.p1(), acs.getId());
        BigDecimal p2 = scoreOf(scoreMap, termPeriods.p2(), acs.getId());
        BigDecimal exam = scoreOf(scoreMap, termPeriods.exam(), acs.getId());

        List<WeightedScore> weighted = new ArrayList<>();
        addWeighted(weighted, p1, termPeriods.p1());
        addWeighted(weighted, p2, termPeriods.p2());
        addWeighted(weighted, exam, termPeriods.exam());

        BigDecimal tot = sumNullable(p1, p2, exam);
        BigDecimal moy = null;
        if (!weighted.isEmpty()) {
            BigDecimal weightSum = weighted.stream()
                    .map(WeightedScore::weight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (weightSum.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal weightedSum = weighted.stream()
                        .map(w -> w.score().multiply(w.weight()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                moy = weightedSum.divide(weightSum, 2, RoundingMode.HALF_UP);
            } else {
                moy = tot.divide(BigDecimal.valueOf(weighted.size()), 2, RoundingMode.HALF_UP);
            }
        }

        return new TermScore(acs.getMaximumPoints(), p1, p2, exam, moy, tot);
    }

    private void addWeighted(List<WeightedScore> weighted, BigDecimal score, AcademicPeriod period) {
        if (score == null || period == null) {
            return;
        }
        BigDecimal ratio = period.getMaximumScoreRatio() != null
                ? BigDecimal.valueOf(period.getMaximumScoreRatio())
                : BigDecimal.ONE;
        weighted.add(new WeightedScore(score, ratio));
    }

    private BigDecimal scoreOf(Map<String, BigDecimal> scoreMap, AcademicPeriod period, UUID curriculumSubjectId) {
        if (period == null) {
            return null;
        }
        return scoreMap.get(period.getId() + "|" + curriculumSubjectId);
    }

    private void fillTermColumns(BulletinSubjectRowDto row, int termIndex, TermScore score) {
        String max = formatScore(score.max());
        String p1 = formatScore(score.p1());
        String p2 = formatScore(score.p2());
        String exam = formatScore(score.exam());
        String moy = formatScore(score.moy());
        String tot = formatScore(score.tot());
        switch (termIndex) {
            case 1 -> {
                row.setT1Max(max);
                row.setT1P1(p1);
                row.setT1P2(p2);
                row.setT1Exam(exam);
                row.setT1Moy(moy);
                row.setT1Rr("");
                row.setT1Tot(tot);
            }
            case 2 -> {
                row.setT2Max(max);
                row.setT2P1(p1);
                row.setT2P2(p2);
                row.setT2Exam(exam);
                row.setT2Moy(moy);
                row.setT2Rr("");
                row.setT2Tot(tot);
            }
            case 3 -> {
                row.setT3Max(max);
                row.setT3P1(p1);
                row.setT3P2(p2);
                row.setT3Exam(exam);
                row.setT3Moy(moy);
                row.setT3Rr("");
                row.setT3Tot(tot);
            }
            default -> {
            }
        }
    }

    private StudentIdentity buildIdentity(
            Enrollment enrollment,
            School school,
            AcademicYear academicYear,
            String classRank
    ) {
        Student student = enrollment.getStudent();
        String fullName = (nullSafe(student.getLastName())
                + (student.getMiddleName() != null && !student.getMiddleName().isBlank()
                ? " " + student.getMiddleName() : "")
                + " " + nullSafe(student.getFirstName())).trim();

        return new StudentIdentity(
                enrollment.getId(),
                student.getStudentNumber(),
                fullName,
                resolveBirthPlace(student),
                student.getBirthDate() != null ? DATE_FORMATTER.format(student.getBirthDate()) : "",
                classroomNamingService.build(enrollment.getClassroom()),
                enrollment.getEnrollmentNumber(),
                buildPhotoPath(enrollment),
                school.getName(),
                school.getCode(),
                school.getAddress() != null ? school.getAddress() : "",
                school.getPrincipalName(),
                academicYear.getCode(),
                classRank,
                null
        );
    }

    private String resolveBirthPlace(Student student) {
        if (student.getBirthCity() != null && student.getBirthCity().getName() != null) {
            return student.getBirthCity().getName();
        }
        if (student.getBirthCommune() != null && student.getBirthCommune().getName() != null) {
            return student.getBirthCommune().getName();
        }
        if (student.getBirthCountry() != null && student.getBirthCountry().getNameFr() != null) {
            return student.getBirthCountry().getNameFr();
        }
        return "";
    }

    private String buildPhotoPath(Enrollment enrollment) {
        if (enrollment.getPhoto() == null) {
            return null;
        }
        return Paths.get(enrollment.getPhoto().getPath(), enrollment.getPhoto().getFileName()).toString();
    }

    private BulletinSubjectRowDto baseRow(StudentIdentity identity) {
        BulletinSubjectRowDto row = new BulletinSubjectRowDto();
        row.setStudentEnrollmentId(identity.studentEnrollmentId());
        row.setStudentNumber(emptyToBlank(identity.studentNumber()));
        row.setStudentFullName(emptyToBlank(identity.studentFullName()));
        row.setBirthPlace(emptyToBlank(identity.birthPlace()));
        row.setBirthDate(emptyToBlank(identity.birthDate()));
        row.setClassroomName(emptyToBlank(identity.classroomName()));
        row.setEnrollmentNumber(emptyToBlank(identity.enrollmentNumber()));
        row.setPhotoPath(emptyToBlank(identity.photoPath()));
        row.setSchoolName(emptyToBlank(identity.schoolName()));
        row.setSchoolCode(emptyToBlank(identity.schoolCode()));
        row.setSchoolAddress(emptyToBlank(identity.schoolAddress()));
        row.setPrincipalName(emptyToBlank(identity.principalName()));
        row.setAcademicYearLabel(emptyToBlank(identity.academicYearLabel()));
        row.setClassRank(emptyToBlank(identity.classRank()));
        row.setSchoolRank(emptyToBlank(identity.schoolRank()));
        row.setRowType("");
        row.setDomainName("");
        row.setBranchName("");
        row.setMaxPoints("");
        row.setT1Max("");
        row.setT1P1("");
        row.setT1P2("");
        row.setT1Exam("");
        row.setT1Moy("");
        row.setT1Rr("");
        row.setT1Tot("");
        row.setT2Max("");
        row.setT2P1("");
        row.setT2P2("");
        row.setT2Exam("");
        row.setT2Moy("");
        row.setT2Rr("");
        row.setT2Tot("");
        row.setT3Max("");
        row.setT3P1("");
        row.setT3P2("");
        row.setT3Exam("");
        row.setT3Moy("");
        row.setT3Rr("");
        row.setT3Tot("");
        row.setYearMax("");
        row.setYearTot("");
        row.setClassAvg("");
        return row;
    }

    private Map<String, Object> buildParameters(Context context, BulletinPrintRequestDto request) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("INCLUDE_COVER_PAGE", Boolean.TRUE.equals(request.getIncludeCoverPage()));
        parameters.put("INCLUDE_SIGNATURES", request.getIncludeSignatures() == null || request.getIncludeSignatures());
        parameters.put("INCLUDE_STUDENT_RANK", Boolean.TRUE.equals(request.getIncludeStudentRank()));
        parameters.put("INCLUDE_CLASS_AVERAGES", Boolean.TRUE.equals(request.getIncludeClassAverages()));
        parameters.put("COVER_TITLE", context.school().getName());
        parameters.put(
                "COVER_SUBTITLE",
                "Année scolaire " + context.academicYear().getCode()
                        + " — " + context.enrollments().size() + " élève(s)"
        );
        parameters.put("T1_LABEL", termLabel(context.termPeriods().get(0), "1er TRIMESTRE"));
        parameters.put("T2_LABEL", termLabel(context.termPeriods().get(1), "2e TRIMESTRE"));
        parameters.put("T3_LABEL", termLabel(context.termPeriods().get(2), "3e TRIMESTRE"));
        parameters.put("PRINT_PLACE", "");
        parameters.put("PRINT_DATE", DATE_FORMATTER.format(LocalDate.now()));
        return parameters;
    }

    private String termLabel(TermPeriods termPeriods, String fallback) {
        if (termPeriods == null || termPeriods.term() == null) {
            return fallback;
        }
        return termPeriods.term().getName() != null ? termPeriods.term().getName() : fallback;
    }

    /**
     * En-têtes de section du bulletin = SubjectDomain (domaine), jamais SubjectSubDomain.
     * Si le domaine n'est pas renseigné sur la matière, on remonte via le sous-domaine.
     */
    private String resolveDomainName(Subject subject) {
        SubjectDomain domain = resolveSubjectDomain(subject);
        if (domain != null && domain.getName() != null && !domain.getName().isBlank()) {
            return domain.getName();
        }
        return "AUTRES";
    }

    private int resolveDomainDisplayOrder(Subject subject) {
        SubjectDomain domain = resolveSubjectDomain(subject);
        return domain != null && domain.getDisplayOrder() != null
                ? domain.getDisplayOrder()
                : Integer.MAX_VALUE;
    }

    private SubjectDomain resolveSubjectDomain(Subject subject) {
        if (subject == null) {
            return null;
        }
        if (subject.getSubjectDomain() != null) {
            return subject.getSubjectDomain();
        }
        if (subject.getSubjectSubDomain() != null) {
            return subject.getSubjectSubDomain().getSubjectDomain();
        }
        return null;
    }

    private String formatScore(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().scale() <= 0
                ? value.toBigInteger().toString()
                : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal sumNullable(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
            }
        }
        return sum;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "x";
        }
        return value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private record Context(
            School school,
            AcademicYear academicYear,
            List<Enrollment> enrollments,
            List<TermPeriods> termPeriods
    ) {
    }

    private record TermPeriods(
            AcademicTerm term,
            AcademicPeriod p1,
            AcademicPeriod p2,
            AcademicPeriod exam
    ) {
        static TermPeriods empty() {
            return new TermPeriods(null, null, null, null);
        }
    }

    private record TermScore(
            BigDecimal max,
            BigDecimal p1,
            BigDecimal p2,
            BigDecimal exam,
            BigDecimal moy,
            BigDecimal tot
    ) {
        static TermScore empty(BigDecimal max) {
            return new TermScore(max, null, null, null, null, null);
        }
    }

    private record WeightedScore(BigDecimal score, BigDecimal weight) {
    }

    private record StudentIdentity(
            UUID studentEnrollmentId,
            String studentNumber,
            String studentFullName,
            String birthPlace,
            String birthDate,
            String classroomName,
            String enrollmentNumber,
            String photoPath,
            String schoolName,
            String schoolCode,
            String schoolAddress,
            String principalName,
            String academicYearLabel,
            String classRank,
            String schoolRank
    ) {
    }
}
