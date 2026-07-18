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
import eclosia.eclosia_organization_service.report.dto.RevenueStatementRowDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_currency.repository.SchoolCurrencyRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueStatementReportService {

    private static final String REPORT_PATH = "report/revenue_statement.jrxml";
    private static final String ECLOSIA_LOGO_PATH = "report/eclosia-logo.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNamingService classroomNamingService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentFeeResolver enrollmentFeeResolver;
    private final PaymentRepository paymentRepository;
    private final SchoolCurrencyRepository schoolCurrencyRepository;

    private record PaidKey(UUID enrollmentId, UUID academicFeeId) {}

    private record CategoryAccumulator(
            String feeCategoryName,
            BigDecimal expected,
            BigDecimal collected,
            Set<UUID> enrollmentIds
    ) {
        CategoryAccumulator(String feeCategoryName) {
            this(feeCategoryName, BigDecimal.ZERO, BigDecimal.ZERO, new HashSet<>());
        }

        CategoryAccumulator addExpected(BigDecimal amount) {
            return new CategoryAccumulator(
                    feeCategoryName,
                    expected.add(amount != null ? amount : BigDecimal.ZERO),
                    collected,
                    enrollmentIds
            );
        }

        CategoryAccumulator addCollected(BigDecimal amount) {
            return new CategoryAccumulator(
                    feeCategoryName,
                    expected,
                    collected.add(amount != null ? amount : BigDecimal.ZERO),
                    enrollmentIds
            );
        }

        CategoryAccumulator addEnrollment(UUID enrollmentId) {
            Set<UUID> copy = new HashSet<>(enrollmentIds);
            if (enrollmentId != null) {
                copy.add(enrollmentId);
            }
            return new CategoryAccumulator(feeCategoryName, expected, collected, copy);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateRevenueStatement(
            UUID schoolId,
            UUID academicYearId,
            List<UUID> cycleIds,
            List<UUID> classroomIds
    ) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        AcademicYearCountryValidator.requireSameCountry(school, academicYear);

        Currency referenceCurrency = resolveSchoolCurrency(schoolId);
        String referenceCurrencyCode = referenceCurrency.getCode();

        Set<UUID> cycleFilter = toIdSet(cycleIds);
        Set<UUID> classroomFilter = toIdSet(classroomIds);
        Set<UUID> allowedClassroomIds = resolveAllowedClassroomIds(schoolId, cycleFilter, classroomFilter);
        if (allowedClassroomIds.isEmpty()) {
            throw new BadRequestException("Aucune classe trouvée pour les filtres sélectionnés");
        }

        Map<PaidKey, BigDecimal> paidByEnrollmentAndFee = new HashMap<>();
        Map<PaymentMethod, BigDecimal> paidByMethod = new EnumMap<>(PaymentMethod.class);
        Map<String, BigDecimal> paidByCurrency = new HashMap<>();
        for (PaymentMethod method : PaymentMethod.values()) {
            paidByMethod.put(method, BigDecimal.ZERO);
        }

        collectPayments(
                schoolId,
                academicYearId,
                allowedClassroomIds,
                referenceCurrencyCode,
                paidByEnrollmentAndFee,
                paidByMethod,
                paidByCurrency
        );

        Map<UUID, CategoryAccumulator> byCategoryId = new LinkedHashMap<>();
        Set<UUID> concernedEnrollmentIds = new HashSet<>();

        List<Enrollment> enrollments = enrollmentRepository
                .findForSchoolAndAcademicYearReport(academicYearId, schoolId);
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

            Map<UUID, List<AcademicFee>> feesByCategory = fees.stream()
                    .filter(fee -> fee.getFeeCategory() != null && fee.getFeeCategory().getId() != null)
                    .filter(fee -> Boolean.TRUE.equals(fee.getActive()))
                    .collect(Collectors.groupingBy(fee -> fee.getFeeCategory().getId(), LinkedHashMap::new, Collectors.toList()));

            if (feesByCategory.isEmpty()) {
                continue;
            }

            concernedEnrollmentIds.add(enrollment.getId());

            for (Map.Entry<UUID, List<AcademicFee>> entry : feesByCategory.entrySet()) {
                UUID categoryId = entry.getKey();
                List<AcademicFee> categoryFees = entry.getValue();
                String categoryName = categoryFees.getFirst().getFeeCategory().getName();

                CategoryAccumulator accumulator = byCategoryId.getOrDefault(
                        categoryId,
                        new CategoryAccumulator(categoryName)
                );

                BigDecimal expected = categoryFees.stream()
                        .map(AcademicFee::getAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                accumulator = accumulator.addExpected(expected).addEnrollment(enrollment.getId());

                BigDecimal collected = BigDecimal.ZERO;
                for (AcademicFee fee : categoryFees) {
                    collected = collected.add(paidByEnrollmentAndFee.getOrDefault(
                            new PaidKey(enrollment.getId(), fee.getId()),
                            BigDecimal.ZERO
                    ));
                }
                accumulator = accumulator.addCollected(collected);
                byCategoryId.put(categoryId, accumulator);
            }
        }

        List<RevenueStatementRowDto> rows = byCategoryId.values().stream()
                .map(accumulator -> mapRow(accumulator, referenceCurrencyCode))
                .sorted(Comparator.comparing(RevenueStatementRowDto::getFeeCategoryName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));

        int rowNumber = 1;
        for (RevenueStatementRowDto row : rows) {
            row.setRowNumber(rowNumber++);
            row.setCategoryStudentCount(row.getStudentCount());
            row.setCategoryExpectedAmountLabel(row.getExpectedAmountLabel());
            row.setCategoryCollectedAmountLabel(row.getCollectedAmountLabel());
            row.setCategoryRecoveryRateLabel(row.getRecoveryRateLabel());
            row.setCategoryRemainingAmountLabel(row.getRemainingAmountLabel());
        }

        BigDecimal totalExpected = rows.stream()
                .map(RevenueStatementRowDto::getExpectedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = rows.stream()
                .map(RevenueStatementRowDto::getCollectedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalExpected.subtract(totalCollected).max(BigDecimal.ZERO);

        Map<String, Object> parameters = buildParameters(
                school,
                academicYear,
                referenceCurrencyCode,
                cycleFilter,
                classroomFilter,
                concernedEnrollmentIds.size(),
                totalExpected,
                totalCollected,
                totalRemaining,
                paidByMethod,
                paidByCurrency
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
            throw new IllegalStateException("Unable to generate revenue statement report", exception);
        }
    }

    private RevenueStatementRowDto mapRow(CategoryAccumulator accumulator, String currencyCode) {
        BigDecimal expected = accumulator.expected();
        BigDecimal collected = accumulator.collected();
        BigDecimal remaining = expected.subtract(collected).max(BigDecimal.ZERO);
        int studentCount = accumulator.enrollmentIds().size();

        RevenueStatementRowDto row = new RevenueStatementRowDto();
        row.setFeeCategoryName(accumulator.feeCategoryName());
        row.setStudentCount(studentCount);
        row.setExpectedAmount(expected);
        row.setExpectedAmountLabel(formatAmount(expected));
        row.setCollectedAmount(collected);
        row.setCollectedAmountLabel(formatAmount(collected));
        row.setRecoveryRateLabel(formatPercent(percentage(collected, expected)));
        row.setRemainingAmount(remaining);
        row.setRemainingAmountLabel(formatAmount(remaining));
        return row;
    }

    private void collectPayments(
            UUID schoolId,
            UUID academicYearId,
            Set<UUID> allowedClassroomIds,
            String referenceCurrencyCode,
            Map<PaidKey, BigDecimal> paidByEnrollmentAndFee,
            Map<PaymentMethod, BigDecimal> paidByMethod,
            Map<String, BigDecimal> paidByCurrency
    ) {
        List<Payment> payments = paymentRepository.findJournalPaymentsWithDetails(schoolId, academicYearId);
        for (Payment payment : payments) {
            if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED) {
                continue;
            }
            if (payment.getEnrollment() == null
                    || payment.getEnrollment().getClassroom() == null
                    || payment.getAcademicFee() == null) {
                continue;
            }
            if (!allowedClassroomIds.contains(payment.getEnrollment().getClassroom().getId())) {
                continue;
            }

            BigDecimal paidReference = toReferenceCurrency(
                    payment.getAmount(),
                    payment.getCurrencyRate(),
                    referenceCurrencyCode
            );
            paidByEnrollmentAndFee.merge(
                    new PaidKey(payment.getEnrollment().getId(), payment.getAcademicFee().getId()),
                    paidReference,
                    BigDecimal::add
            );
            if (payment.getPaymentMethod() != null) {
                paidByMethod.merge(payment.getPaymentMethod(), paidReference, BigDecimal::add);
            }
            paidByCurrency.merge(
                    resolveCurrencyCode(payment.getCurrencyRate()),
                    payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO,
                    BigDecimal::add
            );
        }
    }

    private Map<String, Object> buildParameters(
            School school,
            AcademicYear academicYear,
            String referenceCurrencyCode,
            Set<UUID> cycleFilter,
            Set<UUID> classroomFilter,
            int totalStudents,
            BigDecimal totalExpected,
            BigDecimal totalCollected,
            BigDecimal totalRemaining,
            Map<PaymentMethod, BigDecimal> paidByMethod,
            Map<String, BigDecimal> paidByCurrency
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LOGO_IMAGE", resolveLogoImage(school));
        parameters.put("SCHOOL_NAME", resolveSchoolName(school));
        parameters.put("SCHOOL_ADDRESS", defaultValue(school.getAddress(), "-"));
        parameters.put("SCHOOL_PHONE", defaultValue(school.getPhone(), "-"));
        parameters.put("SCHOOL_EMAIL", defaultValue(school.getEmail(), "-"));
        parameters.put("ACADEMIC_YEAR_LABEL", academicYear.getCode());
        parameters.put("PERIOD_LABEL", buildPeriodLabel(academicYear));
        parameters.put("CYCLE_FILTER_LABEL", resolveCycleFilterLabel(cycleFilter));
        parameters.put("CLASS_FILTER_LABEL", resolveClassroomFilterLabel(classroomFilter));
        parameters.put("PAYMENT_MODE_FILTER_LABEL", "Tous");
        parameters.put("CURRENCY_FILTER_LABEL", referenceCurrencyCode);
        parameters.put("REFERENCE_CURRENCY_CODE", referenceCurrencyCode);
        parameters.put("PRINT_DATETIME", DATETIME_FORMATTER.format(LocalDateTime.now()));

        parameters.put("TOTAL_EXPECTED_LABEL", formatAmount(totalExpected) + " " + referenceCurrencyCode);
        parameters.put("TOTAL_COLLECTED_LABEL", formatAmount(totalCollected) + " " + referenceCurrencyCode);
        parameters.put("TOTAL_RECOVERY_RATE_LABEL", formatPercent(percentage(totalCollected, totalExpected)));
        parameters.put("TOTAL_REMAINING_LABEL", formatAmount(totalRemaining) + " " + referenceCurrencyCode);
        parameters.put("TOTAL_STUDENTS_COUNT", totalStudents);

        parameters.put("GRAND_TOTAL_EXPECTED_LABEL", formatAmount(totalExpected));
        parameters.put("GRAND_TOTAL_COLLECTED_LABEL", formatAmount(totalCollected));
        parameters.put("GRAND_TOTAL_RECOVERY_RATE_LABEL", formatPercent(percentage(totalCollected, totalExpected)));
        parameters.put("GRAND_TOTAL_REMAINING_LABEL", formatAmount(totalRemaining));

        BigDecimal cash = paidByMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
        BigDecimal mobile = paidByMethod.getOrDefault(PaymentMethod.MOBILE_MONEY, BigDecimal.ZERO);
        BigDecimal bank = paidByMethod.getOrDefault(PaymentMethod.BANK_TRANSFER, BigDecimal.ZERO);
        parameters.put("RECAP_CASH_AMOUNT", formatAmount(cash));
        parameters.put("RECAP_CASH_PERCENT", formatPercent(percentage(cash, totalCollected)));
        parameters.put("RECAP_MOBILE_AMOUNT", formatAmount(mobile));
        parameters.put("RECAP_MOBILE_PERCENT", formatPercent(percentage(mobile, totalCollected)));
        parameters.put("RECAP_BANK_AMOUNT", formatAmount(bank));
        parameters.put("RECAP_BANK_PERCENT", formatPercent(percentage(bank, totalCollected)));

        parameters.put("RECAP_CDF_EXPECTED", referenceCurrencyCode.equalsIgnoreCase("CDF")
                ? formatAmount(totalExpected) : formatAmount(BigDecimal.ZERO));
        parameters.put("RECAP_CDF_COLLECTED", formatAmount(paidByCurrency.getOrDefault("CDF", BigDecimal.ZERO)));
        parameters.put("RECAP_CDF_REMAINING", referenceCurrencyCode.equalsIgnoreCase("CDF")
                ? formatAmount(totalRemaining) : "-");
        parameters.put("RECAP_USD_EXPECTED", referenceCurrencyCode.equalsIgnoreCase("USD")
                ? formatAmount(totalExpected) : formatAmount(BigDecimal.ZERO));
        parameters.put("RECAP_USD_COLLECTED", formatAmount(paidByCurrency.getOrDefault("USD", BigDecimal.ZERO)));
        parameters.put("RECAP_USD_REMAINING", referenceCurrencyCode.equalsIgnoreCase("USD")
                ? formatAmount(totalRemaining) : "-");
        parameters.put("RECAP_EUR_EXPECTED", referenceCurrencyCode.equalsIgnoreCase("EUR")
                ? formatAmount(totalExpected) : formatAmount(BigDecimal.ZERO));
        parameters.put("RECAP_EUR_COLLECTED", formatAmount(paidByCurrency.getOrDefault("EUR", BigDecimal.ZERO)));
        parameters.put("RECAP_EUR_REMAINING", referenceCurrencyCode.equalsIgnoreCase("EUR")
                ? formatAmount(totalRemaining) : "-");
        parameters.put("RECAP_CURRENCY_TOTAL_REFERENCE", formatAmount(totalCollected) + " " + referenceCurrencyCode);

        parameters.put(
                "CURRENCY_NOTE",
                "NB : Le solde représente le reste à recouvrer. "
                        + "Le taux de recouvrement = (montant encaissé ÷ montant attendu) × 100. "
                        + "Montants exprimés en " + referenceCurrencyCode + "."
        );
        return parameters;
    }

    private Set<UUID> resolveAllowedClassroomIds(
            UUID schoolId,
            Set<UUID> cycleFilter,
            Set<UUID> classroomFilter
    ) {
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

    private String buildPeriodLabel(AcademicYear academicYear) {
        if (academicYear.getStartDate() == null || academicYear.getEndDate() == null) {
            return "-";
        }
        return DATE_FORMATTER.format(academicYear.getStartDate())
                + " au "
                + DATE_FORMATTER.format(academicYear.getEndDate());
    }

    private String resolveCycleFilterLabel(Set<UUID> cycleFilter) {
        if (cycleFilter.isEmpty()) {
            return "Tous les cycles";
        }
        List<String> names = academicCycleRepository.findAllOrdered().stream()
                .filter(cycle -> cycleFilter.contains(cycle.getId()))
                .map(AcademicCycle::getName)
                .toList();
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private String resolveClassroomFilterLabel(Set<UUID> classroomFilter) {
        if (classroomFilter.isEmpty()) {
            return "Toutes les classes";
        }
        List<String> names = classroomRepository.findAllById(classroomFilter).stream()
                .map(classroomNamingService::build)
                .sorted()
                .toList();
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private Currency resolveSchoolCurrency(UUID schoolId) {
        return schoolCurrencyRepository.findActiveDefaultBySchoolId(schoolId)
                .map(schoolCurrency -> schoolCurrency.getCurrency())
                .orElseThrow(() -> new BadRequestException(
                        "Aucune devise par défaut active n'est configurée pour l'école."
                ));
    }

    private Set<UUID> toIdSet(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal safePart = part != null ? part : BigDecimal.ZERO;
        return safePart.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private String formatPercent(BigDecimal value) {
        return formatAmount(value) + " %";
    }

    private String resolveCurrencyCode(CurrencyRate currencyRate) {
        if (currencyRate != null && currencyRate.getTargetCurrency() != null) {
            return currencyRate.getTargetCurrency().getCode();
        }
        return "USD";
    }

    private BigDecimal toReferenceCurrency(
            BigDecimal amount,
            CurrencyRate currencyRate,
            String referenceCurrencyCode
    ) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (currencyRate == null
                || currencyRate.getTargetCurrency() == null
                || currencyRate.getSourceCurrency() == null) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        String targetCode = currencyRate.getTargetCurrency().getCode();
        String sourceCode = currencyRate.getSourceCurrency().getCode();
        if (referenceCurrencyCode.equalsIgnoreCase(targetCode)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (referenceCurrencyCode.equalsIgnoreCase(sourceCode)
                && currencyRate.getRate() != null
                && currencyRate.getRate().compareTo(BigDecimal.ZERO) > 0) {
            return amount.divide(currencyRate.getRate(), 2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
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

    private String formatAmount(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
