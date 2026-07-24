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
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.city.repository.CityRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.commune.entity.Commune;
import eclosia.eclosia_organization_service.commune.repository.CommuneRepository;
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
import eclosia.eclosia_organization_service.state.entity.State;
import eclosia.eclosia_organization_service.state.repository.StateRepository;
import eclosia.eclosia_organization_service.student.entity.Student;
import eclosia.eclosia_organization_service.student_grade.entity.StudentGrade;
import eclosia.eclosia_organization_service.student_grade.repository.StudentGradeRepository;
import eclosia.eclosia_organization_service.subject.entity.Subject;
import eclosia.eclosia_organization_service.subject_domain.entity.SubjectDomain;
import eclosia.eclosia_organization_service.subject_sub_domain.entity.SubjectSubDomain;
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
    private static final String FLAG_PATH = "report/drc_flag.png";
    private static final String ARMS_PATH = "report/drc_arms.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ROW_DOMAIN = "DOMAIN_HEADER";
    private static final String ROW_SUBJECT_GROUP = "SUBJECT_GROUP";
    private static final String ROW_SUBJECT = "SUBJECT";
    private static final String ROW_SUBTOTAL = "SUBTOTAL";
    private static final String ROW_GENERAL = "GENERAL_MAXIMA";

    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicTermRepository academicTermRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final AcademicCurriculumRepository academicCurriculumRepository;
    private final AcademicCurriculumSubjectRepository academicCurriculumSubjectRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final CityRepository cityRepository;
    private final CommuneRepository communeRepository;
    private final StateRepository stateRepository;
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
        // Seules les périodes active=true entrent dans le calcul / affichage du bulletin.
        List<AcademicPeriod> allPeriods = termIds.isEmpty()
                ? List.of()
                : academicPeriodRepository.findByAcademicTerm_IdInOrderByDisplayOrderAsc(termIds).stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActive()))
                        .toList();

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
        SchoolLocation schoolLocation = resolveSchoolLocation(context.school());

        Map<UUID, BigDecimal> yearTotalsByEnrollment = new HashMap<>();
        Map<UUID, EnrollmentTotals> totalsByEnrollment = new HashMap<>();
        Map<UUID, Integer> classmatesCount = context.enrollments().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClassroom().getId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

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

            TermScoreAgg t1Agg = TermScoreAgg.zero();
            TermScoreAgg t2Agg = TermScoreAgg.zero();
            TermScoreAgg t3Agg = TermScoreAgg.zero();
            for (AcademicCurriculumSubject acs : subjects) {
                t1Agg.add(computeTermScore(acs, context.termPeriods().get(0), scoreMap));
                t2Agg.add(computeTermScore(acs, context.termPeriods().get(1), scoreMap));
                t3Agg.add(computeTermScore(acs, context.termPeriods().get(2), scoreMap));
            }

            EnrollmentTotals totals = new EnrollmentTotals(
                    t1Agg.toTermScore(),
                    t2Agg.toTermScore(),
                    t3Agg.toTermScore()
            );
            totalsByEnrollment.put(enrollment.getId(), totals);
            yearTotalsByEnrollment.put(enrollment.getId(), totals.yearTot());
        }

        Map<UUID, String> classRanks = Boolean.TRUE.equals(request.getIncludeStudentRank())
                || request.getIncludeStudentRank() == null
                ? computeClassRanks(context.enrollments(), yearTotalsByEnrollment)
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

            EnrollmentTotals totals = totalsByEnrollment.getOrDefault(
                    enrollment.getId(),
                    EnrollmentTotals.empty()
            );

            StudentIdentity identity = buildIdentity(
                    enrollment,
                    context.school(),
                    context.academicYear(),
                    schoolLocation,
                    classRanks.getOrDefault(enrollment.getId(), ""),
                    String.valueOf(classmatesCount.getOrDefault(enrollment.getClassroom().getId(), 0)),
                    buildPercentages(totals)
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
                            .thenComparing(acs -> resolveSubDomainDisplayOrder(acs.getSubject()))
                            .thenComparing(acs -> acs.getDisplayOrder() != null ? acs.getDisplayOrder() : Integer.MAX_VALUE))
                    .collect(Collectors.groupingBy(
                            acs -> resolveDomainName(acs.getSubject()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            BigDecimal generalYear = BigDecimal.ZERO;
            BigDecimal generalYearMax = BigDecimal.ZERO;
            TermScoreAgg generalT1 = TermScoreAgg.zero();
            TermScoreAgg generalT2 = TermScoreAgg.zero();
            TermScoreAgg generalT3 = TermScoreAgg.zero();

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

                Map<String, List<AcademicCurriculumSubject>> byGroup = domainEntry.getValue().stream()
                        .collect(Collectors.groupingBy(
                                acs -> resolveSubDomainName(acs.getSubject()),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

                for (Map.Entry<String, List<AcademicCurriculumSubject>> groupEntry : byGroup.entrySet()) {
                    String groupName = groupEntry.getKey();
                    boolean hasGroup = groupName != null && !groupName.isBlank();
                    if (hasGroup) {
                        BulletinSubjectRowDto groupHeader = baseRow(identity);
                        groupHeader.setRowType(ROW_SUBJECT_GROUP);
                        groupHeader.setDomainName(domainEntry.getKey());
                        groupHeader.setBranchName(groupName.toUpperCase());
                        rows.add(groupHeader);
                    }

                    BigDecimal groupYear = BigDecimal.ZERO;
                    BigDecimal groupYearMax = BigDecimal.ZERO;
                    TermScoreAgg groupT1 = TermScoreAgg.zero();
                    TermScoreAgg groupT2 = TermScoreAgg.zero();
                    TermScoreAgg groupT3 = TermScoreAgg.zero();

                    for (AcademicCurriculumSubject acs : groupEntry.getValue()) {
                        TermScore t1 = computeTermScore(acs, context.termPeriods().get(0), scoreMap);
                        TermScore t2 = computeTermScore(acs, context.termPeriods().get(1), scoreMap);
                        TermScore t3 = computeTermScore(acs, context.termPeriods().get(2), scoreMap);
                        BigDecimal yearTot = nullToZero(sumNullable(t1.tot(), t2.tot(), t3.tot()));
                        BigDecimal yearMax = nullToZero(t1.trimMax())
                                .add(nullToZero(t2.trimMax()))
                                .add(nullToZero(t3.trimMax()));

                        BulletinSubjectRowDto row = baseRow(identity);
                        row.setRowType(ROW_SUBJECT);
                        row.setDomainName(domainEntry.getKey());
                        row.setBranchName(acs.getSubject().getName() != null
                                ? acs.getSubject().getName()
                                : "");
                        row.setMaxPoints(formatScore(t1.max()));
                        fillTermColumns(row, 1, t1);
                        fillTermColumns(row, 2, t2);
                        fillTermColumns(row, 3, t3);
                        row.setYearMax(formatScore(yearMax));
                        row.setYearTot(formatScore(yearTot));
                        rows.add(row);

                        groupT1.add(t1);
                        groupT2.add(t2);
                        groupT3.add(t3);
                        groupYear = groupYear.add(yearTot);
                        groupYearMax = groupYearMax.add(yearMax);

                        generalT1.add(t1);
                        generalT2.add(t2);
                        generalT3.add(t3);
                        generalYear = generalYear.add(yearTot);
                        generalYearMax = generalYearMax.add(yearMax);
                    }

                    BulletinSubjectRowDto subtotal = baseRow(identity);
                    subtotal.setRowType(ROW_SUBTOTAL);
                    subtotal.setDomainName(domainEntry.getKey());
                    subtotal.setBranchName("Sous-total");
                    subtotal.setMaxPoints(formatScore(groupT1.maxPeriod));
                    fillTermColumns(subtotal, 1, groupT1.toTermScore());
                    fillTermColumns(subtotal, 2, groupT2.toTermScore());
                    fillTermColumns(subtotal, 3, groupT3.toTermScore());
                    subtotal.setYearMax(formatScore(groupYearMax));
                    subtotal.setYearTot(formatScore(groupYear));
                    rows.add(subtotal);
                }
            }

            BulletinSubjectRowDto general = baseRow(identity);
            general.setRowType(ROW_GENERAL);
            general.setBranchName("Maxima généraux");
            general.setMaxPoints(formatScore(generalT1.maxPeriod));
            fillTermColumns(general, 1, generalT1.toTermScore());
            fillTermColumns(general, 2, generalT2.toTermScore());
            fillTermColumns(general, 3, generalT3.toTermScore());
            general.setYearMax(formatScore(generalYearMax));
            general.setYearTot(formatScore(generalYear));
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
                ranks.put(enrollment.getId(), String.valueOf(position));
                position++;
            }
        }
        return ranks;
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
        BigDecimal maxPeriod = acs.getMaximumPoints() != null ? acs.getMaximumPoints() : BigDecimal.ZERO;
        // Formule officielle IGE : MAX EX = 2 × MAX période ; MAX TRIM = 2×MAX + MAX EX
        BigDecimal examMax = maxPeriod.multiply(BigDecimal.valueOf(2));
        BigDecimal trimMax = maxPeriod.multiply(BigDecimal.valueOf(2)).add(examMax);

        if (termPeriods == null || termPeriods.term() == null) {
            return new TermScore(maxPeriod, examMax, trimMax, null, null, null, null);
        }

        BigDecimal p1 = scoreOf(scoreMap, termPeriods.p1(), acs.getId());
        BigDecimal p2 = scoreOf(scoreMap, termPeriods.p2(), acs.getId());
        BigDecimal exam = scoreOf(scoreMap, termPeriods.exam(), acs.getId());
        BigDecimal tot = sumNullable(p1, p2, exam);

        return new TermScore(maxPeriod, examMax, trimMax, p1, p2, exam, tot);
    }

    private BigDecimal scoreOf(Map<String, BigDecimal> scoreMap, AcademicPeriod period, UUID curriculumSubjectId) {
        if (period == null) {
            return null;
        }
        return scoreMap.get(period.getId() + "|" + curriculumSubjectId);
    }

    private void fillTermColumns(BulletinSubjectRowDto row, int termIndex, TermScore score) {
        String p1 = formatScore(score.p1());
        String p2 = formatScore(score.p2());
        String examMax = formatScore(score.examMax());
        String exam = formatScore(score.exam());
        String trimMax = formatScore(score.trimMax());
        String tot = formatScore(score.tot());
        switch (termIndex) {
            case 1 -> {
                row.setT1P1(p1);
                row.setT1P2(p2);
                row.setT1ExamMax(examMax);
                row.setT1Exam(exam);
                row.setT1TrimMax(trimMax);
                row.setT1Tot(tot);
            }
            case 2 -> {
                row.setT2P1(p1);
                row.setT2P2(p2);
                row.setT2ExamMax(examMax);
                row.setT2Exam(exam);
                row.setT2TrimMax(trimMax);
                row.setT2Tot(tot);
            }
            case 3 -> {
                row.setT3P1(p1);
                row.setT3P2(p2);
                row.setT3ExamMax(examMax);
                row.setT3Exam(exam);
                row.setT3TrimMax(trimMax);
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
            SchoolLocation location,
            String classRank,
            String studentCount,
            ColumnPercentages percentages
    ) {
        Student student = enrollment.getStudent();
        String fullName = (nullSafe(student.getLastName())
                + (student.getMiddleName() != null && !student.getMiddleName().isBlank()
                ? " " + student.getMiddleName() : "")
                + " " + nullSafe(student.getFirstName())).trim();

        String bulletinTitle = "BULLETIN DE L'ELEVE";
        if (enrollment.getClassroom() != null
                && enrollment.getClassroom().getAcademicLevel() != null
                && enrollment.getClassroom().getAcademicLevel().getAcademicCycle() != null
                && enrollment.getClassroom().getAcademicLevel().getAcademicCycle().getName() != null) {
            bulletinTitle = "BULLETIN DE L'ELEVE "
                    + enrollment.getClassroom().getAcademicLevel().getAcademicCycle().getName().toUpperCase();
        }

        return new StudentIdentity(
                enrollment.getId(),
                student.getStudentNumber(),
                fullName,
                formatGender(student.getGender()),
                resolveBirthPlace(student),
                student.getBirthDate() != null ? DATE_FORMATTER.format(student.getBirthDate()) : "",
                classroomNamingService.build(enrollment.getClassroom()),
                enrollment.getEnrollmentNumber() != null
                        ? enrollment.getEnrollmentNumber()
                        : student.getStudentNumber(),
                school.getName(),
                school.getCode(),
                location.provinceName(),
                location.cityName(),
                location.communeName(),
                school.getPrincipalName(),
                academicYear.getCode(),
                bulletinTitle,
                classRank,
                studentCount,
                percentages
        );
    }

    private ColumnPercentages buildPercentages(EnrollmentTotals totals) {
        TermScore t1 = totals.t1();
        TermScore t2 = totals.t2();
        TermScore t3 = totals.t3();
        return new ColumnPercentages(
                formatPercentage(t1.p1(), t1.max()),
                formatPercentage(t1.p2(), t1.max()),
                formatPercentage(t1.exam(), t1.examMax()),
                formatPercentage(t1.tot(), t1.trimMax()),
                formatPercentage(t2.p1(), t2.max()),
                formatPercentage(t2.p2(), t2.max()),
                formatPercentage(t2.exam(), t2.examMax()),
                formatPercentage(t2.tot(), t2.trimMax()),
                formatPercentage(t3.p1(), t3.max()),
                formatPercentage(t3.p2(), t3.max()),
                formatPercentage(t3.exam(), t3.examMax()),
                formatPercentage(t3.tot(), t3.trimMax()),
                formatPercentage(totals.yearTot(), totals.yearMax())
        );
    }

    private String formatPercentage(BigDecimal obtained, BigDecimal maximum) {
        if (obtained == null || maximum == null || maximum.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        return formatScore(obtained
                .multiply(BigDecimal.valueOf(100))
                .divide(maximum, 2, RoundingMode.HALF_UP)) + " %";
    }

    private SchoolLocation resolveSchoolLocation(School school) {
        String cityName = "";
        String provinceName = "";
        String communeName = "";

        if (school.getCityId() != null) {
            City city = cityRepository.findById(school.getCityId()).orElse(null);
            if (city != null) {
                cityName = nullSafe(city.getName());
                if (city.getProvinceId() != null) {
                    State state = stateRepository.findById(city.getProvinceId()).orElse(null);
                    if (state != null) {
                        provinceName = nullSafe(state.getName());
                    }
                }
            }
        }
        if (school.getCommuneId() != null) {
            Commune commune = communeRepository.findById(school.getCommuneId()).orElse(null);
            if (commune != null) {
                communeName = nullSafe(commune.getName());
            }
        }
        return new SchoolLocation(provinceName, cityName, communeName);
    }

    private String formatGender(String gender) {
        if (gender == null) {
            return "";
        }
        return switch (gender.trim().toUpperCase()) {
            case "MALE", "M", "MASCULIN" -> "M";
            case "FEMALE", "F", "FEMININ", "FÉMININ" -> "F";
            default -> gender;
        };
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

    private BulletinSubjectRowDto baseRow(StudentIdentity identity) {
        BulletinSubjectRowDto row = new BulletinSubjectRowDto();
        row.setStudentEnrollmentId(identity.studentEnrollmentId());
        row.setStudentNumber(emptyToBlank(identity.studentNumber()));
        row.setStudentFullName(emptyToBlank(identity.studentFullName()));
        row.setGender(emptyToBlank(identity.gender()));
        row.setBirthPlace(emptyToBlank(identity.birthPlace()));
        row.setBirthDate(emptyToBlank(identity.birthDate()));
        row.setClassroomName(emptyToBlank(identity.classroomName()));
        row.setEnrollmentNumber(emptyToBlank(identity.enrollmentNumber()));
        row.setSchoolName(emptyToBlank(identity.schoolName()));
        row.setSchoolCode(emptyToBlank(identity.schoolCode()));
        row.setProvinceName(emptyToBlank(identity.provinceName()));
        row.setCityName(emptyToBlank(identity.cityName()));
        row.setCommuneName(emptyToBlank(identity.communeName()));
        row.setPrincipalName(emptyToBlank(identity.principalName()));
        row.setAcademicYearLabel(emptyToBlank(identity.academicYearLabel()));
        row.setBulletinTitle(emptyToBlank(identity.bulletinTitle()));
        row.setClassRank(emptyToBlank(identity.classRank()));
        row.setStudentCount(emptyToBlank(identity.studentCount()));
        ColumnPercentages pct = identity.percentages();
        row.setT1P1Percentage(emptyToBlank(pct.t1P1()));
        row.setT1P2Percentage(emptyToBlank(pct.t1P2()));
        row.setT1ExamPercentage(emptyToBlank(pct.t1Exam()));
        row.setT1TotPercentage(emptyToBlank(pct.t1Tot()));
        row.setT2P1Percentage(emptyToBlank(pct.t2P1()));
        row.setT2P2Percentage(emptyToBlank(pct.t2P2()));
        row.setT2ExamPercentage(emptyToBlank(pct.t2Exam()));
        row.setT2TotPercentage(emptyToBlank(pct.t2Tot()));
        row.setT3P1Percentage(emptyToBlank(pct.t3P1()));
        row.setT3P2Percentage(emptyToBlank(pct.t3P2()));
        row.setT3ExamPercentage(emptyToBlank(pct.t3Exam()));
        row.setT3TotPercentage(emptyToBlank(pct.t3Tot()));
        row.setYearPercentage(emptyToBlank(pct.year()));
        row.setPercentage(emptyToBlank(pct.year()));
        row.setRowType("");
        row.setDomainName("");
        row.setBranchName("");
        row.setMaxPoints("");
        row.setT1P1("");
        row.setT1P2("");
        row.setT1ExamMax("");
        row.setT1Exam("");
        row.setT1TrimMax("");
        row.setT1Tot("");
        row.setT2P1("");
        row.setT2P2("");
        row.setT2ExamMax("");
        row.setT2Exam("");
        row.setT2TrimMax("");
        row.setT2Tot("");
        row.setT3P1("");
        row.setT3P2("");
        row.setT3ExamMax("");
        row.setT3Exam("");
        row.setT3TrimMax("");
        row.setT3Tot("");
        row.setYearMax("");
        row.setYearTot("");
        return row;
    }

    private Map<String, Object> buildParameters(Context context, BulletinPrintRequestDto request) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("INCLUDE_COVER_PAGE", Boolean.TRUE.equals(request.getIncludeCoverPage()));
        parameters.put("COVER_TITLE", context.school().getName());
        parameters.put(
                "COVER_SUBTITLE",
                "Année scolaire " + context.academicYear().getCode()
                        + " — " + context.enrollments().size() + " élève(s)"
        );
        parameters.put("FLAG_IMAGE", resolveClasspathImage(FLAG_PATH));
        parameters.put("ARMS_IMAGE", resolveClasspathImage(ARMS_PATH));
        parameters.put(
                "PRINT_PLACE",
                resolveSchoolLocation(context.school()).cityName()
        );
        parameters.put("PRINT_DATE", DATE_FORMATTER.format(LocalDate.now()));
        return parameters;
    }

    private Object resolveClasspathImage(String path) {
        java.net.URL url = getClass().getClassLoader().getResource(path);
        return url;
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

    private String resolveSubDomainName(Subject subject) {
        if (subject == null || subject.getSubjectSubDomain() == null) {
            return "";
        }
        SubjectSubDomain subDomain = subject.getSubjectSubDomain();
        return subDomain.getName() != null ? subDomain.getName() : "";
    }

    private int resolveDomainDisplayOrder(Subject subject) {
        SubjectDomain domain = resolveSubjectDomain(subject);
        return domain != null && domain.getDisplayOrder() != null
                ? domain.getDisplayOrder()
                : Integer.MAX_VALUE;
    }

    private int resolveSubDomainDisplayOrder(Subject subject) {
        if (subject == null || subject.getSubjectSubDomain() == null) {
            return Integer.MAX_VALUE;
        }
        Integer order = subject.getSubjectSubDomain().getDisplayOrder();
        return order != null ? order : Integer.MAX_VALUE;
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
        boolean any = false;
        for (BigDecimal value : values) {
            if (value != null) {
                sum = sum.add(value);
                any = true;
            }
        }
        return any ? sum : null;
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
            BigDecimal examMax,
            BigDecimal trimMax,
            BigDecimal p1,
            BigDecimal p2,
            BigDecimal exam,
            BigDecimal tot
    ) {
    }

    /** Accumulateur pour sous-totaux / maxima généraux (somme du sous-ensemble). */
    private static final class TermScoreAgg {
        private BigDecimal maxPeriod = BigDecimal.ZERO;
        private BigDecimal examMax = BigDecimal.ZERO;
        private BigDecimal trimMax = BigDecimal.ZERO;
        private BigDecimal p1 = BigDecimal.ZERO;
        private BigDecimal p2 = BigDecimal.ZERO;
        private BigDecimal exam = BigDecimal.ZERO;
        private BigDecimal tot = BigDecimal.ZERO;
        private boolean anyP1;
        private boolean anyP2;
        private boolean anyExam;
        private boolean anyTot;

        static TermScoreAgg zero() {
            return new TermScoreAgg();
        }

        void add(TermScore score) {
            maxPeriod = maxPeriod.add(nullSafeBd(score.max()));
            examMax = examMax.add(nullSafeBd(score.examMax()));
            trimMax = trimMax.add(nullSafeBd(score.trimMax()));
            if (score.p1() != null) {
                p1 = p1.add(score.p1());
                anyP1 = true;
            }
            if (score.p2() != null) {
                p2 = p2.add(score.p2());
                anyP2 = true;
            }
            if (score.exam() != null) {
                exam = exam.add(score.exam());
                anyExam = true;
            }
            if (score.tot() != null) {
                tot = tot.add(score.tot());
                anyTot = true;
            }
        }

        TermScore toTermScore() {
            return new TermScore(
                    maxPeriod,
                    examMax,
                    trimMax,
                    anyP1 ? p1 : null,
                    anyP2 ? p2 : null,
                    anyExam ? exam : null,
                    anyTot ? tot : null
            );
        }

        private static BigDecimal nullSafeBd(BigDecimal value) {
            return value != null ? value : BigDecimal.ZERO;
        }
    }

    private record EnrollmentTotals(TermScore t1, TermScore t2, TermScore t3) {
        static EnrollmentTotals empty() {
            TermScore empty = new TermScore(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, null, null, null
            );
            return new EnrollmentTotals(empty, empty, empty);
        }

        BigDecimal yearTot() {
            return nullToZeroStatic(t1.tot())
                    .add(nullToZeroStatic(t2.tot()))
                    .add(nullToZeroStatic(t3.tot()));
        }

        BigDecimal yearMax() {
            return nullToZeroStatic(t1.trimMax())
                    .add(nullToZeroStatic(t2.trimMax()))
                    .add(nullToZeroStatic(t3.trimMax()));
        }

        private static BigDecimal nullToZeroStatic(BigDecimal value) {
            return value != null ? value : BigDecimal.ZERO;
        }
    }

    private record SchoolLocation(String provinceName, String cityName, String communeName) {
    }

    private record ColumnPercentages(
            String t1P1,
            String t1P2,
            String t1Exam,
            String t1Tot,
            String t2P1,
            String t2P2,
            String t2Exam,
            String t2Tot,
            String t3P1,
            String t3P2,
            String t3Exam,
            String t3Tot,
            String year
    ) {
    }

    private record StudentIdentity(
            UUID studentEnrollmentId,
            String studentNumber,
            String studentFullName,
            String gender,
            String birthPlace,
            String birthDate,
            String classroomName,
            String enrollmentNumber,
            String schoolName,
            String schoolCode,
            String provinceName,
            String cityName,
            String communeName,
            String principalName,
            String academicYearLabel,
            String bulletinTitle,
            String classRank,
            String studentCount,
            ColumnPercentages percentages
    ) {
    }
}
