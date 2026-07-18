package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.enrollment.service.EnrollmentFeeResolver;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import eclosia.eclosia_organization_service.payment_installment.repository.PaymentInstallmentRepository;
import eclosia.eclosia_organization_service.report.dto.PaymentRecoveryDashboardRowDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_currency.entity.SchoolCurrency;
import eclosia.eclosia_organization_service.school_currency.repository.SchoolCurrencyRepository;
import eclosia.eclosia_organization_service.student.entity.Student;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentRecoveryReportService {

    private static final String REPORT_PATH = "report/payment_recovery_dashboard.jrxml";
    private static final String ECLOSIA_LOGO_PATH = "report/eclosia-logo.png";
    private static final BigDecimal EPSILON = new BigDecimal("0.0001");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNamingService classroomNamingService;
    private final PaymentInstallmentRepository paymentInstallmentRepository;
    private final SchoolCurrencyRepository schoolCurrencyRepository;
    private final EnrollmentFeeResolver enrollmentFeeResolver;

    private record PaidKey(UUID enrollmentId, UUID academicFeeId) {}

    private record PaymentRecap(
            Map<PaidKey, BigDecimal> paidByEnrollmentAndFee,
            Map<PaymentMethod, BigDecimal> paidReferenceByMethod,
            Map<String, BigDecimal> paidByCurrencyOriginal
    ) {}

    private record StudentCategorySummary(
            UUID enrollmentId,
            String cycleName,
            int cycleDisplayOrder,
            String feeCategoryName,
            String classroomName,
            int levelOrder,
            String studentMatricule,
            String studentFullName,
            BigDecimal expected,
            BigDecimal paid,
            BigDecimal remaining,
            String paymentStatusCode,
            String paymentStatusLabel
    ) {}

    private static String categoryKey(String cycleName, String feeCategoryName) {
        return cycleName + "|" + feeCategoryName;
    }

    private static String classKey(String cycleName, String feeCategoryName, String classroomName) {
        return cycleName + "|" + feeCategoryName + "|" + classroomName;
    }

    @Transactional(readOnly = true)
    public byte[] generatePaymentRecoveryDashboard(
            UUID schoolId,
            UUID academicYearId,
            List<UUID> trancheIds,
            List<UUID> cycleIds,
            List<UUID> classroomIds
    ) {
        if (schoolId == null || academicYearId == null) {
            throw new BadRequestException("schoolId et academicYearId sont obligatoires");
        }
        if (trancheIds == null || trancheIds.isEmpty() || trancheIds.stream().allMatch(Objects::isNull)) {
            throw new BadRequestException("Au moins une tranche doit etre selectionnee");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        AcademicYearCountryValidator.requireSameCountry(school, academicYear);

        Currency referenceCurrency = resolveReferenceCurrency(schoolId);
        String referenceCurrencyCode = referenceCurrency.getCode();

        Set<UUID> trancheFilter = toIdSet(trancheIds);
        Set<UUID> cycleFilter = toIdSet(cycleIds);
        Set<UUID> classroomFilter = toIdSet(classroomIds);
        Set<UUID> allowedClassroomIds = resolveAllowedClassroomIds(schoolId, cycleFilter, classroomFilter);
        if (allowedClassroomIds.isEmpty()) {
            throw new BadRequestException("Aucune classe trouvee pour les filtres selectionnes");
        }

        PaymentRecap paymentRecap = buildPaymentRecap(
                schoolId,
                academicYearId,
                trancheFilter,
                allowedClassroomIds,
                referenceCurrencyCode
        );
        Map<PaidKey, BigDecimal> paidByEnrollmentAndFee = paymentRecap.paidByEnrollmentAndFee();

        Map<UUID, Integer> cycleOrderById = buildCycleOrderIndex();

        List<StudentCategorySummary> summaries = buildStudentSummaries(
                schoolId,
                academicYearId,
                trancheFilter,
                allowedClassroomIds,
                paidByEnrollmentAndFee,
                cycleOrderById
        );

        if (summaries.isEmpty()) {
            throw new BadRequestException("Aucun eleve avec des frais pour les tranches et filtres selectionnes");
        }

        Map<String, Set<UUID>> enrollmentsByCycle = new HashMap<>();
        Map<String, Set<UUID>> unpaidByCycle = new HashMap<>();
        Map<String, Set<UUID>> paidEnrollmentByCycle = new HashMap<>();
        Map<String, Set<UUID>> enrollmentsByCategoryKey = new HashMap<>();
        Map<String, Set<UUID>> enrollmentsByClassKey = new HashMap<>();
        Map<String, Set<UUID>> unpaidByClassKey = new HashMap<>();
        Map<String, Set<UUID>> paidEnrollmentByClassKey = new HashMap<>();
        Map<String, BigDecimal> expectedByCycle = new HashMap<>();
        Map<String, BigDecimal> paidByCycle = new HashMap<>();
        Map<String, BigDecimal> expectedByCategoryKey = new HashMap<>();
        Map<String, BigDecimal> paidByCategoryKey = new HashMap<>();
        Map<String, BigDecimal> expectedByClassKey = new HashMap<>();
        Map<String, BigDecimal> paidByClassKey = new HashMap<>();
        Map<String, BigDecimal> unpaidAmountByClassKey = new HashMap<>();
        Map<String, BigDecimal> expectedByClass = new HashMap<>();
        Map<String, BigDecimal> paidByClass = new HashMap<>();

        for (StudentCategorySummary summary : summaries) {
            String categoryKey = categoryKey(summary.cycleName(), summary.feeCategoryName());
            String classKey = classKey(summary.cycleName(), summary.feeCategoryName(), summary.classroomName());

            enrollmentsByCycle.computeIfAbsent(summary.cycleName(), key -> new HashSet<>())
                    .add(summary.enrollmentId());
            enrollmentsByCategoryKey.computeIfAbsent(categoryKey, key -> new HashSet<>())
                    .add(summary.enrollmentId());
            enrollmentsByClassKey.computeIfAbsent(classKey, key -> new HashSet<>())
                    .add(summary.enrollmentId());

            expectedByCycle.merge(summary.cycleName(), summary.expected(), BigDecimal::add);
            paidByCycle.merge(summary.cycleName(), summary.paid(), BigDecimal::add);
            expectedByCategoryKey.merge(categoryKey, summary.expected(), BigDecimal::add);
            paidByCategoryKey.merge(categoryKey, summary.paid(), BigDecimal::add);
            expectedByClassKey.merge(classKey, summary.expected(), BigDecimal::add);
            paidByClassKey.merge(classKey, summary.paid(), BigDecimal::add);
            expectedByClass.merge(summary.classroomName(), summary.expected(), BigDecimal::add);
            paidByClass.merge(summary.classroomName(), summary.paid(), BigDecimal::add);

            if (summary.remaining().compareTo(EPSILON) > 0) {
                unpaidByCycle.computeIfAbsent(summary.cycleName(), key -> new HashSet<>())
                        .add(summary.enrollmentId());
                unpaidByClassKey.computeIfAbsent(classKey, key -> new HashSet<>())
                        .add(summary.enrollmentId());
                unpaidAmountByClassKey.merge(classKey, summary.remaining(), BigDecimal::add);
            } else {
                paidEnrollmentByCycle.computeIfAbsent(summary.cycleName(), key -> new HashSet<>())
                        .add(summary.enrollmentId());
                paidEnrollmentByClassKey.computeIfAbsent(classKey, key -> new HashSet<>())
                        .add(summary.enrollmentId());
            }
        }

        List<StudentCategorySummary> reportSummaries = summaries.stream()
                .sorted(Comparator
                        .comparing(StudentCategorySummary::cycleDisplayOrder)
                        .thenComparing(StudentCategorySummary::cycleName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StudentCategorySummary::feeCategoryName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StudentCategorySummary::levelOrder)
                        .thenComparing(StudentCategorySummary::classroomName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(summary -> statusOrder(summary.paymentStatusCode()))
                        .thenComparing(StudentCategorySummary::studentFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<PaymentRecoveryDashboardRowDto> rows = new ArrayList<>();
        Map<String, Integer> rowCounterByClassKey = new HashMap<>();
        for (StudentCategorySummary summary : reportSummaries) {
            String categoryKey = categoryKey(summary.cycleName(), summary.feeCategoryName());
            String classKey = classKey(summary.cycleName(), summary.feeCategoryName(), summary.classroomName());

            BigDecimal expCycle = expectedByCycle.getOrDefault(summary.cycleName(), BigDecimal.ZERO);
            BigDecimal paidCycle = paidByCycle.getOrDefault(summary.cycleName(), BigDecimal.ZERO);
            BigDecimal remCycle = expCycle.subtract(paidCycle);
            BigDecimal expCat = expectedByCategoryKey.getOrDefault(categoryKey, BigDecimal.ZERO);
            BigDecimal paidCat = paidByCategoryKey.getOrDefault(categoryKey, BigDecimal.ZERO);
            BigDecimal remCat = expCat.subtract(paidCat);
            BigDecimal expClass = expectedByClassKey.getOrDefault(classKey, BigDecimal.ZERO);
            BigDecimal paidClass = paidByClassKey.getOrDefault(classKey, BigDecimal.ZERO);
            BigDecimal remClass = expClass.subtract(paidClass);
            BigDecimal unpaidClassAmount = unpaidAmountByClassKey.getOrDefault(classKey, BigDecimal.ZERO);

            PaymentRecoveryDashboardRowDto row = new PaymentRecoveryDashboardRowDto();
            row.setRowNumber(rowCounterByClassKey.merge(classKey, 1, Integer::sum));
            row.setCycleName(summary.cycleName());
            row.setCycleDisplayOrder(summary.cycleDisplayOrder());
            row.setFeeCategoryName(summary.feeCategoryName());
            row.setClassroomName(summary.classroomName());
            row.setStudentMatricule(summary.studentMatricule());
            row.setStudentFullName(summary.studentFullName());

            row.setExpectedAmount(summary.expected());
            row.setPaidAmountReference(summary.paid());
            row.setRemainingAmount(summary.remaining());
            row.setExpectedAmountLabel(formatAmount(summary.expected(), referenceCurrencyCode));
            row.setPaidAmountLabel(formatAmount(summary.paid(), referenceCurrencyCode));
            row.setPaidPercentageLabel(formatPercent(percentage(summary.paid(), summary.expected())));
            row.setRemainingAmountLabel(formatAmount(summary.remaining(), referenceCurrencyCode));
            row.setRemainingPercentageLabel(formatPercent(percentage(summary.remaining(), summary.expected())));
            row.setPaymentStatusCode(summary.paymentStatusCode());
            row.setPaymentStatusLabel(summary.paymentStatusLabel());

            row.setCycleEffectiveCount(enrollmentsByCycle.getOrDefault(summary.cycleName(), Set.of()).size());
            row.setCyclePaidCount(paidEnrollmentByCycle.getOrDefault(summary.cycleName(), Set.of()).size());
            row.setCycleUnpaidCount(unpaidByCycle.getOrDefault(summary.cycleName(), Set.of()).size());
            row.setCycleExpectedTotalLabel(formatAmount(expCycle, referenceCurrencyCode));
            row.setCyclePaidTotalLabel(formatAmount(paidCycle, referenceCurrencyCode));
            row.setCycleRecoveryRateLabel(formatPercent(percentage(paidCycle, expCycle)));
            row.setCycleRemainingTotalLabel(formatAmount(remCycle.max(BigDecimal.ZERO), referenceCurrencyCode));

            row.setCategoryEffectiveCount(enrollmentsByCategoryKey.getOrDefault(categoryKey, Set.of()).size());
            row.setCategoryExpectedTotalLabel(formatAmount(expCat, referenceCurrencyCode));
            row.setCategoryPaidTotalLabel(formatAmount(paidCat, referenceCurrencyCode));
            row.setCategoryRecoveryRateLabel(formatPercent(percentage(paidCat, expCat)));
            row.setCategoryRemainingTotalLabel(formatAmount(remCat.max(BigDecimal.ZERO), referenceCurrencyCode));
            row.setCategoryRemainingRateLabel(formatPercent(percentage(remCat.max(BigDecimal.ZERO), expCat)));

            row.setClassTotalEnrollmentCount(enrollmentsByClassKey.getOrDefault(classKey, Set.of()).size());
            row.setClassUnpaidCount(unpaidByClassKey.getOrDefault(classKey, Set.of()).size());
            row.setClassPaidCount(paidEnrollmentByClassKey.getOrDefault(classKey, Set.of()).size());
            row.setClassUnpaidTotalLabel(formatAmount(unpaidClassAmount, referenceCurrencyCode));
            row.setClassEffectiveCount(unpaidByClassKey.getOrDefault(classKey, Set.of()).size());
            row.setClassExpectedTotalLabel(formatAmount(expClass, referenceCurrencyCode));
            row.setClassPaidTotalLabel(formatAmount(paidClass, referenceCurrencyCode));
            row.setClassRecoveryRateLabel(formatPercent(percentage(paidClass, expClass)));
            row.setClassRemainingTotalLabel(formatAmount(remClass.max(BigDecimal.ZERO), referenceCurrencyCode));
            row.setClassRemainingRateLabel(formatPercent(percentage(remClass.max(BigDecimal.ZERO), expClass)));

            rows.add(row);
        }

        rows.sort(Comparator
                .comparing(PaymentRecoveryDashboardRowDto::getCycleDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PaymentRecoveryDashboardRowDto::getCycleName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PaymentRecoveryDashboardRowDto::getFeeCategoryName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(PaymentRecoveryDashboardRowDto::getClassroomName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> statusOrder(row.getPaymentStatusCode()))
                .thenComparing(PaymentRecoveryDashboardRowDto::getStudentFullName, String.CASE_INSENSITIVE_ORDER));

        Set<UUID> allEnrollmentIds = summaries.stream()
                .map(StudentCategorySummary::enrollmentId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<UUID> unpaidEnrollmentIds = summaries.stream()
                .filter(summary -> summary.remaining().compareTo(EPSILON) > 0)
                .map(StudentCategorySummary::enrollmentId)
                .collect(Collectors.toCollection(HashSet::new));

        BigDecimal totalExpected = summaries.stream()
                .map(StudentCategorySummary::expected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = summaries.stream()
                .map(StudentCategorySummary::paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalExpected.subtract(totalPaid);

        String bestClassroomName = "";
        BigDecimal bestClassRate = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : expectedByClass.entrySet()) {
            String classroomName = entry.getKey();
            BigDecimal expected = entry.getValue();
            BigDecimal paid = paidByClass.getOrDefault(classroomName, BigDecimal.ZERO);
            if (expected.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal rate = percentage(paid, expected);
            if (bestClassroomName.isBlank() || rate.compareTo(bestClassRate) > 0) {
                bestClassroomName = classroomName;
                bestClassRate = rate;
            }
        }

        List<PaymentInstallment> trancheEntities = paymentInstallmentRepository.findAllById(trancheFilter);
        trancheEntities.sort(Comparator.comparing(PaymentInstallment::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));
        String trancheLabel = trancheEntities.isEmpty()
                ? "-"
                : trancheEntities.stream().map(PaymentInstallment::getName).collect(Collectors.joining(", "));

        Set<String> categoryNames = summaries.stream()
                .map(StudentCategorySummary::feeCategoryName)
                .collect(Collectors.toCollection(HashSet::new));
        String feeCategoryLabel = categoryNames.size() == 1
                ? categoryNames.iterator().next()
                : String.join(", ", categoryNames.stream().sorted().toList());

        Map<String, Object> parameters = buildParameters(
                school,
                academicYear,
                referenceCurrencyCode,
                cycleFilter,
                classroomFilter,
                trancheLabel,
                feeCategoryLabel,
                allEnrollmentIds.size(),
                unpaidEnrollmentIds.size(),
                totalExpected,
                totalPaid,
                totalRemaining,
                bestClassroomName,
                bestClassRate,
                paymentRecap,
                referenceCurrencyCode
        );

        try (InputStream reportStream = new ClassPathResource(REPORT_PATH).getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    new JRBeanCollectionDataSource(rows)
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            throw new IllegalStateException("Unable to generate payment recovery dashboard report", exception);
        }
    }

    private Map<UUID, Integer> buildCycleOrderIndex() {
        List<AcademicCycle> cycles = academicCycleRepository.findAllOrdered();
        Map<UUID, Integer> orderById = new HashMap<>();
        int rank = 0;
        for (AcademicCycle cycle : cycles) {
            if (cycle.getId() != null) {
                orderById.put(cycle.getId(), rank++);
            }
        }
        return orderById;
    }

    private List<StudentCategorySummary> buildStudentSummaries(
            UUID schoolId,
            UUID academicYearId,
            Set<UUID> trancheFilter,
            Set<UUID> allowedClassroomIds,
            Map<PaidKey, BigDecimal> paidByEnrollmentAndFee,
            Map<UUID, Integer> cycleOrderById
    ) {
        List<Enrollment> enrollments = enrollmentRepository.findForSchoolAndAcademicYearReport(academicYearId, schoolId);
        List<StudentCategorySummary> summaries = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            if (enrollment == null || enrollment.getClassroom() == null || enrollment.getClassroom().getId() == null) {
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
                continue;
            }
            if (!allowedClassroomIds.contains(enrollment.getClassroom().getId())) {
                continue;
            }

            List<AcademicFee> fees = enrollmentFeeResolver.resolveFees(enrollment.getId());
            if (fees == null || fees.isEmpty()) {
                continue;
            }

            List<AcademicFee> relevantFees = fees.stream()
                    .filter(fee -> fee.getPaymentInstallment() != null)
                    .filter(fee -> trancheFilter.contains(fee.getPaymentInstallment().getId()))
                    .toList();
            if (relevantFees.isEmpty()) {
                continue;
            }

            Map<String, List<AcademicFee>> feesByCategoryName = relevantFees.stream()
                    .filter(fee -> fee.getFeeCategory() != null)
                    .collect(Collectors.groupingBy(fee -> fee.getFeeCategory().getName()));

            Classroom classroom = enrollment.getClassroom();
            AcademicCycle academicCycle = classroom.getAcademicLevel() != null
                    ? classroom.getAcademicLevel().getAcademicCycle()
                    : null;
            String cycleName = academicCycle != null && academicCycle.getName() != null
                    ? academicCycle.getName()
                    : "-";
            int cycleDisplayOrder = academicCycle != null && academicCycle.getId() != null
                    ? cycleOrderById.getOrDefault(academicCycle.getId(), Integer.MAX_VALUE)
                    : Integer.MAX_VALUE;
            String classroomName = classroomNamingService.build(classroom);
            int levelOrder = classroom.getAcademicLevel() != null && classroom.getAcademicLevel().getLevelOrder() != null
                    ? classroom.getAcademicLevel().getLevelOrder()
                    : Integer.MAX_VALUE;
            String studentMatricule = defaultValue(
                    enrollment.getStudent() != null ? enrollment.getStudent().getStudentNumber() : null,
                    "-"
            );
            String studentFullName = buildStudentFullName(enrollment.getStudent());

            for (Map.Entry<String, List<AcademicFee>> entry : feesByCategoryName.entrySet()) {
                String feeCategoryName = entry.getKey();
                List<AcademicFee> categoryFees = entry.getValue();

                BigDecimal expected = categoryFees.stream()
                        .map(AcademicFee::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (expected.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal paid = BigDecimal.ZERO;
                for (AcademicFee fee : categoryFees) {
                    paid = paid.add(paidByEnrollmentAndFee.getOrDefault(
                            new PaidKey(enrollment.getId(), fee.getId()),
                            BigDecimal.ZERO
                    ));
                }
                BigDecimal remaining = expected.subtract(paid).max(BigDecimal.ZERO);

                String paymentStatusCode;
                if (remaining.compareTo(EPSILON) <= 0) {
                    paymentStatusCode = "PAID";
                } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    paymentStatusCode = "PARTIAL";
                } else {
                    paymentStatusCode = "UNPAID";
                }

                String paymentStatusLabel = switch (paymentStatusCode) {
                    case "PAID" -> "Soldé (100 %)";
                    case "PARTIAL" -> "Paiement partiel";
                    default -> "Non payé";
                };

                summaries.add(new StudentCategorySummary(
                        enrollment.getId(),
                        cycleName,
                        cycleDisplayOrder,
                        feeCategoryName,
                        classroomName,
                        levelOrder,
                        studentMatricule,
                        studentFullName,
                        expected,
                        paid,
                        remaining,
                        paymentStatusCode,
                        paymentStatusLabel
                ));
            }
        }

        return summaries;
    }

    private PaymentRecap buildPaymentRecap(
            UUID schoolId,
            UUID academicYearId,
            Set<UUID> trancheFilter,
            Set<UUID> allowedClassroomIds,
            String referenceCurrencyCode
    ) {
        List<Payment> payments = paymentRepository.findJournalPaymentsWithDetails(schoolId, academicYearId);
        Map<PaidKey, BigDecimal> paidByEnrollmentAndFee = new HashMap<>();
        Map<PaymentMethod, BigDecimal> paidReferenceByMethod = new EnumMap<>(PaymentMethod.class);
        Map<String, BigDecimal> paidByCurrencyOriginal = new HashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            paidReferenceByMethod.put(method, BigDecimal.ZERO);
        }

        for (Payment payment : payments) {
            if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED) {
                continue;
            }
            if (payment.getAcademicFee() == null || payment.getAcademicFee().getPaymentInstallment() == null) {
                continue;
            }
            if (!trancheFilter.contains(payment.getAcademicFee().getPaymentInstallment().getId())) {
                continue;
            }
            if (payment.getEnrollment() == null || payment.getEnrollment().getClassroom() == null) {
                continue;
            }
            if (!allowedClassroomIds.contains(payment.getEnrollment().getClassroom().getId())) {
                continue;
            }

            PaidKey key = new PaidKey(payment.getEnrollment().getId(), payment.getAcademicFee().getId());
            BigDecimal paidReference = toReferenceCurrency(
                    payment.getAmount(),
                    payment.getCurrencyRate(),
                    referenceCurrencyCode
            );
            paidByEnrollmentAndFee.merge(key, paidReference, BigDecimal::add);
            paidReferenceByMethod.merge(payment.getPaymentMethod(), paidReference, BigDecimal::add);
            String currencyCode = resolveCurrencyCode(payment.getCurrencyRate());
            BigDecimal originalAmount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            paidByCurrencyOriginal.merge(currencyCode, originalAmount, BigDecimal::add);
        }

        return new PaymentRecap(paidByEnrollmentAndFee, paidReferenceByMethod, paidByCurrencyOriginal);
    }

    private Map<String, Object> buildParameters(
            School school,
            AcademicYear academicYear,
            String referenceCurrencyCode,
            Set<UUID> cycleFilter,
            Set<UUID> classroomFilter,
            String trancheLabel,
            String feeCategoryLabel,
            int totalEnrollmentCount,
            int unpaidEnrollmentCount,
            BigDecimal totalExpected,
            BigDecimal totalPaid,
            BigDecimal totalRemaining,
            String bestClassroomName,
            BigDecimal bestClassRate,
            PaymentRecap paymentRecap,
            String currencyCode
    ) {
        Map<PaymentMethod, BigDecimal> paidReferenceByMethod = paymentRecap.paidReferenceByMethod();
        Map<String, BigDecimal> paidByCurrencyOriginal = paymentRecap.paidByCurrencyOriginal();
        BigDecimal grandTotalPaidReference = paidReferenceByMethod.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LOGO_IMAGE", resolveLogoImage(school));
        parameters.put("SCHOOL_NAME", resolveSchoolName(school));
        parameters.put("SCHOOL_ADDRESS", defaultValue(school.getAddress(), "-"));
        parameters.put("SCHOOL_PHONE", defaultValue(school.getPhone(), "-"));
        parameters.put("SCHOOL_EMAIL", defaultValue(school.getEmail(), "-"));
        parameters.put("ACADEMIC_YEAR_LABEL", academicYear.getCode());
        parameters.put("PERIOD_LABEL", DATE_FORMATTER.format(academicYear.getStartDate())
                + " au " + DATE_FORMATTER.format(academicYear.getEndDate()));
        parameters.put("CYCLE_LABEL", resolveCycleFilterLabel(cycleFilter));
        parameters.put("CLASS_FILTER_LABEL", resolveClassroomFilterLabel(classroomFilter));
        parameters.put("TRANCHE_LABEL", trancheLabel);
        parameters.put("FEE_CATEGORY_LABEL", feeCategoryLabel);
        parameters.put("REFERENCE_CURRENCY_CODE", referenceCurrencyCode);
        parameters.put("PRINT_DATETIME", DATETIME_FORMATTER.format(LocalDateTime.now()));

        parameters.put("BEST_CLASS_NAME", defaultValue(bestClassroomName, "-"));
        parameters.put("BEST_CLASS_RECOVERY_RATE_LABEL", formatPercent(bestClassRate));

        parameters.put("TOTAL_EFFECTIF_LABEL", String.valueOf(totalEnrollmentCount));
        parameters.put("TOTAL_UNPAID_COUNT_LABEL", String.valueOf(unpaidEnrollmentCount));
        parameters.put("TOTAL_EXPECTED_AMOUNT_LABEL", formatAmount(totalExpected, currencyCode));
        parameters.put("TOTAL_PAID_AMOUNT_LABEL", formatAmount(totalPaid, currencyCode));
        parameters.put("TOTAL_RECOVERY_RATE_LABEL", formatPercent(percentage(totalPaid, totalExpected)));
        parameters.put("TOTAL_REMAINING_AMOUNT_LABEL", formatAmount(totalRemaining.max(BigDecimal.ZERO), currencyCode));
        parameters.put("TOTAL_REMAINING_RATE_LABEL", formatPercent(percentage(totalRemaining.max(BigDecimal.ZERO), totalExpected)));
        parameters.put("TOTAL_UNPAID_PERCENT_LABEL", formatPercent(percentage(
                totalRemaining.max(BigDecimal.ZERO),
                totalExpected
        )));

        parameters.put("RECAP_CASH_AMOUNT_LABEL", formatAmount(
                paidReferenceByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO), currencyCode));
        parameters.put("RECAP_CASH_PERCENT_LABEL", formatPercent(percentage(
                paidReferenceByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO), totalExpected)));
        parameters.put("RECAP_MOBILE_AMOUNT_LABEL", formatAmount(
                paidReferenceByMethod.getOrDefault(PaymentMethod.MOBILE_MONEY, BigDecimal.ZERO), currencyCode));
        parameters.put("RECAP_MOBILE_PERCENT_LABEL", formatPercent(percentage(
                paidReferenceByMethod.getOrDefault(PaymentMethod.MOBILE_MONEY, BigDecimal.ZERO), totalExpected)));
        parameters.put("RECAP_BANK_AMOUNT_LABEL", formatAmount(
                paidReferenceByMethod.getOrDefault(PaymentMethod.BANK_TRANSFER, BigDecimal.ZERO), currencyCode));
        parameters.put("RECAP_BANK_PERCENT_LABEL", formatPercent(percentage(
                paidReferenceByMethod.getOrDefault(PaymentMethod.BANK_TRANSFER, BigDecimal.ZERO), totalExpected)));

        parameters.put("RECAP_CURRENCY_USD", formatAmount(paidByCurrencyOriginal.getOrDefault("USD", BigDecimal.ZERO), "USD"));
        parameters.put("RECAP_CURRENCY_CDF", formatAmount(paidByCurrencyOriginal.getOrDefault("CDF", BigDecimal.ZERO), "CDF"));
        parameters.put("RECAP_CURRENCY_EUR", formatAmount(paidByCurrencyOriginal.getOrDefault("EUR", BigDecimal.ZERO), "EUR"));
        parameters.put("RECAP_CURRENCY_TOTAL_REFERENCE", formatAmount(grandTotalPaidReference, currencyCode));

        return parameters;
    }

    private Currency resolveReferenceCurrency(UUID schoolId) {
        return schoolCurrencyRepository.findActiveDefaultBySchoolId(schoolId)
                .map(SchoolCurrency::getCurrency)
                .orElseThrow(() -> new BadRequestException(
                        "Aucune devise par defaut active n'est configuree pour l'ecole de l'annee scolaire."
                ));
    }

    private Set<UUID> toIdSet(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> resolveAllowedClassroomIds(UUID schoolId, Set<UUID> cycleFilter, Set<UUID> classroomFilter) {
        if (!classroomFilter.isEmpty()) {
            if (cycleFilter.isEmpty()) {
                return classroomFilter;
            }
            return classroomRepository.findBySchoolIdWithLevelAndCycle(schoolId).stream()
                    .filter(classroom -> classroomFilter.contains(classroom.getId()))
                    .filter(classroom -> classroom.getAcademicLevel() != null
                            && classroom.getAcademicLevel().getAcademicCycle() != null
                            && cycleFilter.contains(classroom.getAcademicLevel().getAcademicCycle().getId()))
                    .map(Classroom::getId)
                    .collect(Collectors.toCollection(HashSet::new));
        }

        return classroomRepository.findBySchoolIdWithLevelAndCycle(schoolId).stream()
                .filter(classroom -> cycleFilter.isEmpty()
                        || (classroom.getAcademicLevel() != null
                        && classroom.getAcademicLevel().getAcademicCycle() != null
                        && cycleFilter.contains(classroom.getAcademicLevel().getAcademicCycle().getId())))
                .map(Classroom::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String resolveCycleFilterLabel(Set<UUID> cycleFilter) {
        if (cycleFilter.isEmpty()) {
            return "Tous les cycles";
        }
        List<String> names = academicCycleRepository.findAllById(cycleFilter).stream()
                .map(AcademicCycle::getName)
                .sorted()
                .toList();
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private String resolveClassroomFilterLabel(Set<UUID> classroomFilter) {
        if (classroomFilter.isEmpty()) {
            return "Toutes les classes";
        }
        return classroomRepository.findAllById(classroomFilter).stream()
                .map(classroomNamingService::build)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal safePart = part != null ? part : BigDecimal.ZERO;
        return safePart.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private Object resolveLogoImage(School school) {
        if (school.getLogo() != null && !school.getLogo().isBlank()) {
            return Paths.get(school.getLogo()).toFile();
        }
        return getClass().getClassLoader().getResource(ECLOSIA_LOGO_PATH);
    }

    private String resolveSchoolName(School school) {
        if (school.getName() != null && !school.getName().isBlank()) {
            return school.getName();
        }
        if (school.getShortName() != null && !school.getShortName().isBlank()) {
            return school.getShortName();
        }
        return defaultValue(school.getCode(), "-");
    }

    private int statusOrder(String paymentStatusCode) {
        return switch (paymentStatusCode) {
            case "UNPAID" -> 0;
            case "PARTIAL" -> 1;
            case "PAID" -> 2;
            default -> 3;
        };
    }

    private String resolveCurrencyCode(CurrencyRate currencyRate) {
        if (currencyRate != null && currencyRate.getTargetCurrency() != null && currencyRate.getTargetCurrency().getCode() != null) {
            return currencyRate.getTargetCurrency().getCode();
        }
        return "USD";
    }

    private String buildStudentFullName(Student student) {
        if (student == null) {
            return "-";
        }
        StringBuilder name = new StringBuilder(defaultValue(student.getLastName(), ""));
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            name.append(" ").append(student.getMiddleName().trim());
        }
        if (student.getFirstName() != null && !student.getFirstName().isBlank()) {
            name.append(" ").append(student.getFirstName().trim());
        }
        String fullName = name.toString().trim();
        return fullName.isBlank() ? "-" : fullName;
    }

    private String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private BigDecimal toReferenceCurrency(BigDecimal amount, CurrencyRate currencyRate, String referenceCurrencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currencyRate == null || currencyRate.getTargetCurrency() == null || currencyRate.getSourceCurrency() == null) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        String targetCode = currencyRate.getTargetCurrency().getCode();
        String sourceCode = currencyRate.getSourceCurrency().getCode();
        BigDecimal rate = currencyRate.getRate();
        if (referenceCurrencyCode.equalsIgnoreCase(targetCode)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (referenceCurrencyCode.equalsIgnoreCase(sourceCode)
                && rate != null
                && rate.compareTo(BigDecimal.ZERO) > 0) {
            return amount.divide(rate, 2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatAmount(BigDecimal amount, String currencyCode) {
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(safe) + (currencyCode != null && !currencyCode.isBlank() ? (" " + currencyCode) : "");
    }

    private String formatPercent(BigDecimal percent) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(percent != null ? percent : BigDecimal.ZERO) + " %";
    }
}
