package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.common.validation.AcademicYearCountryValidator;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import eclosia.eclosia_organization_service.report.dto.PaymentJournalRowDto;
import eclosia.eclosia_organization_service.school.entity.School;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentJournalReportService {

    private static final String REPORT_PATH = "report/payment_journal.jrxml";
    private static final String ECLOSIA_LOGO_PATH = "report/eclosia-logo.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final PaymentRepository paymentRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNamingService classroomNamingService;
    private final SchoolCurrencyRepository schoolCurrencyRepository;

    @Transactional(readOnly = true)
    public byte[] generatePaymentJournal(
            UUID schoolId,
            UUID academicYearId,
            LocalDate startDate,
            LocalDate endDate,
            List<UUID> cycleIds,
            List<UUID> classroomIds
    ) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Les dates de début et de fin sont obligatoires.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (cycleIds == null || cycleIds.isEmpty() || cycleIds.stream().allMatch(id -> id == null)) {
            throw new BadRequestException("Au moins un cycle doit être sélectionné.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        AcademicYearCountryValidator.requireSameCountry(school, academicYear);

        Currency referenceCurrency = resolveAcademicYearCurrency(schoolId);

        Set<UUID> cycleFilter = toIdSet(cycleIds);
        Set<UUID> classroomFilter = toIdSet(classroomIds);

        List<Payment> allPayments = deduplicatePayments(
                paymentRepository.findJournalPaymentsWithDetails(schoolId, academicYearId)
        );

        Map<UUID, Integer> cycleOrderById = buildCycleOrderIndex();

        List<Payment> payments = allPayments
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .filter(payment -> matchesDateRange(payment, startDate, endDate))
                .filter(payment -> matchesPaymentFilters(payment, cycleFilter, classroomFilter))
                .sorted(Comparator
                        .comparingInt((Payment payment) -> resolveCycleDisplayOrder(payment, cycleOrderById))
                        .thenComparing(payment -> payment.getAcademicFee().getFeeCategory().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(payment -> payment.getEnrollment().getClassroom().getAcademicLevel().getLevelOrder())
                        .thenComparing(payment -> classroomNamingService.build(payment.getEnrollment().getClassroom()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Payment::getCreatedAt)
                        .thenComparing(Payment::getPaymentDate))
                .toList();

        List<PaymentJournalRowDto> rows = payments.stream()
                .map(payment -> mapRow(payment, referenceCurrency, cycleOrderById))
                .collect(Collectors.toList());
        enrichGroupMetadata(rows, referenceCurrency.getCode());

        Map<String, Object> parameters = buildParameters(
                school,
                academicYear,
                referenceCurrency,
                startDate,
                endDate,
                cycleFilter,
                classroomFilter,
                payments,
                rows
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
            throw new IllegalStateException("Unable to generate payment journal report", exception);
        }
    }

    private Map<String, Object> buildParameters(
            School school,
            AcademicYear academicYear,
            Currency referenceCurrency,
            LocalDate startDate,
            LocalDate endDate,
            Set<UUID> cycleFilter,
            Set<UUID> classroomFilter,
            List<Payment> payments,
            List<PaymentJournalRowDto> rows
    ) {
        String referenceCurrencyCode = referenceCurrency.getCode();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LOGO_IMAGE", resolveLogoImage(school));
        parameters.put("SCHOOL_NAME", resolveSchoolName(school));
        parameters.put("SCHOOL_ADDRESS", defaultValue(school.getAddress(), "-"));
        parameters.put("SCHOOL_PHONE", defaultValue(school.getPhone(), "-"));
        parameters.put("SCHOOL_EMAIL", defaultValue(school.getEmail(), "-"));
        parameters.put("ACADEMIC_YEAR_LABEL", academicYear.getCode());
        parameters.put("PERIOD_LABEL", DATE_FORMATTER.format(startDate) + " au " + DATE_FORMATTER.format(endDate));
        parameters.put("CLASS_FILTER_LABEL", resolveClassroomFilterLabel(classroomFilter));
        parameters.put("CYCLE_FILTER_LABEL", resolveCycleFilterLabel(cycleFilter));
        parameters.put("FEE_CATEGORY_FILTER_LABEL", "Toutes");
        parameters.put("PAYMENT_METHOD_FILTER_LABEL", "Tous");
        parameters.put("CURRENCY_FILTER_LABEL", referenceCurrencyCode);
        parameters.put("REFERENCE_CURRENCY_CODE", referenceCurrencyCode);
        parameters.put("PRINT_DATETIME", DATETIME_FORMATTER.format(LocalDateTime.now()));
        parameters.put("TOTAL_PAYMENTS", payments.size());

        appendSummaryParameters(parameters, payments, referenceCurrencyCode);
        return parameters;
    }

    private void appendSummaryParameters(
            Map<String, Object> parameters,
            List<Payment> payments,
            String referenceCurrencyCode
    ) {
        Map<PaymentMethod, Map<String, BigDecimal>> byMethodCurrency = new EnumMap<>(PaymentMethod.class);
        Map<String, BigDecimal> byCurrency = new HashMap<>();
        Map<PaymentMethod, BigDecimal> totalByMethodInReference = new EnumMap<>(PaymentMethod.class);
        BigDecimal grandTotalReference = BigDecimal.ZERO;

        for (PaymentMethod method : PaymentMethod.values()) {
            byMethodCurrency.put(method, new HashMap<>());
            totalByMethodInReference.put(method, BigDecimal.ZERO);
        }

        for (Payment payment : payments) {
            String currencyCode = resolveCurrencyCode(payment.getCurrencyRate());
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            BigDecimal amountInReference = toReferenceCurrency(
                    amount,
                    payment.getCurrencyRate(),
                    referenceCurrencyCode
            );

            byCurrency.merge(currencyCode, amount, BigDecimal::add);
            byMethodCurrency.get(payment.getPaymentMethod()).merge(currencyCode, amount, BigDecimal::add);
            totalByMethodInReference.merge(payment.getPaymentMethod(), amountInReference, BigDecimal::add);
            grandTotalReference = grandTotalReference.add(amountInReference);
        }

        putMethodSummary(
                parameters,
                "CASH",
                byMethodCurrency.get(PaymentMethod.CASH),
                totalByMethodInReference.get(PaymentMethod.CASH),
                referenceCurrencyCode
        );
        putMethodSummary(
                parameters,
                "MOBILE",
                byMethodCurrency.get(PaymentMethod.MOBILE_MONEY),
                totalByMethodInReference.get(PaymentMethod.MOBILE_MONEY),
                referenceCurrencyCode
        );
        putMethodSummary(
                parameters,
                "BANK",
                byMethodCurrency.get(PaymentMethod.BANK_TRANSFER),
                totalByMethodInReference.get(PaymentMethod.BANK_TRANSFER),
                referenceCurrencyCode
        );

        parameters.put("RECAP_CURRENCY_USD", formatAmount(byCurrency.getOrDefault("USD", BigDecimal.ZERO)));
        parameters.put("RECAP_CURRENCY_CDF", formatAmount(byCurrency.getOrDefault("CDF", BigDecimal.ZERO)));
        parameters.put("RECAP_CURRENCY_EUR", formatAmount(byCurrency.getOrDefault("EUR", BigDecimal.ZERO)));
        parameters.put(
                "RECAP_CURRENCY_TOTAL_USD",
                formatAmount(grandTotalReference) + " " + referenceCurrencyCode
        );

        parameters.put("GRAND_TOTAL_USD", formatAmount(byCurrency.getOrDefault("USD", BigDecimal.ZERO)) + " USD");
        parameters.put("GRAND_TOTAL_CDF", formatAmount(byCurrency.getOrDefault("CDF", BigDecimal.ZERO)) + " CDF");
        parameters.put("GRAND_TOTAL_EUR", formatAmount(byCurrency.getOrDefault("EUR", BigDecimal.ZERO)) + " EUR");
        parameters.put(
                "GRAND_TOTAL_EQUIVALENT_USD",
                formatAmount(grandTotalReference) + " " + referenceCurrencyCode
        );
    }

    private void putMethodSummary(
            Map<String, Object> parameters,
            String prefix,
            Map<String, BigDecimal> amountsByCurrency,
            BigDecimal totalReference,
            String referenceCurrencyCode
    ) {
        BigDecimal usd = amountsByCurrency.getOrDefault("USD", BigDecimal.ZERO);
        BigDecimal cdf = amountsByCurrency.getOrDefault("CDF", BigDecimal.ZERO);
        BigDecimal eur = amountsByCurrency.getOrDefault("EUR", BigDecimal.ZERO);

        parameters.put("RECAP_" + prefix + "_USD", formatAmount(usd));
        parameters.put("RECAP_" + prefix + "_CDF", formatAmount(cdf));
        parameters.put("RECAP_" + prefix + "_EUR", formatAmount(eur));
        parameters.put("RECAP_" + prefix + "_TOTAL", formatAmount(totalReference) + " " + referenceCurrencyCode);
    }

    private List<Payment> deduplicatePayments(List<Payment> payments) {
        Map<UUID, Payment> uniquePayments = new LinkedHashMap<>();
        for (Payment payment : payments) {
            if (payment != null && payment.getId() != null) {
                uniquePayments.putIfAbsent(payment.getId(), payment);
            }
        }
        return List.copyOf(uniquePayments.values());
    }

    private boolean matchesPaymentFilters(Payment payment, Set<UUID> cycleFilter, Set<UUID> classroomFilter) {
        if (payment.getEnrollment() == null || payment.getEnrollment().getClassroom() == null) {
            return false;
        }
        UUID classroomId = payment.getEnrollment().getClassroom().getId();
        if (!classroomFilter.isEmpty() && (classroomId == null || !classroomFilter.contains(classroomId))) {
            return false;
        }
        if (cycleFilter.isEmpty()) {
            return true;
        }
        return resolvePaymentCycleIds(payment).stream().anyMatch(cycleFilter::contains);
    }

    private List<UUID> resolvePaymentCycleIds(Payment payment) {
        Set<UUID> cycleIds = new HashSet<>();
        if (payment.getAcademicFee() != null) {
            addCycleId(cycleIds, payment.getAcademicFee().getAcademicCycle());
            if (payment.getAcademicFee().getAcademicLevel() != null) {
                addCycleId(cycleIds, payment.getAcademicFee().getAcademicLevel().getAcademicCycle());
            }
            if (payment.getAcademicFee().getAcademicSection() != null) {
                addCycleId(cycleIds, payment.getAcademicFee().getAcademicSection().getAcademicCycle());
            }
        }
        if (payment.getEnrollment().getClassroom().getAcademicLevel() != null) {
            addCycleId(
                    cycleIds,
                    payment.getEnrollment().getClassroom().getAcademicLevel().getAcademicCycle()
            );
        }
        if (payment.getEnrollment().getClassroom().getAcademicSection() != null) {
            addCycleId(
                    cycleIds,
                    payment.getEnrollment().getClassroom().getAcademicSection().getAcademicCycle()
            );
        }
        return List.copyOf(cycleIds);
    }

    private void addCycleId(Set<UUID> cycleIds, AcademicCycle cycle) {
        if (cycle != null && cycle.getId() != null) {
            cycleIds.add(cycle.getId());
        }
    }

    private AcademicCycle resolveReportCycle(Payment payment) {
        if (payment.getAcademicFee() != null && payment.getAcademicFee().getAcademicCycle() != null) {
            return payment.getAcademicFee().getAcademicCycle();
        }
        if (payment.getEnrollment() != null
                && payment.getEnrollment().getClassroom() != null
                && payment.getEnrollment().getClassroom().getAcademicLevel() != null) {
            return payment.getEnrollment().getClassroom().getAcademicLevel().getAcademicCycle();
        }
        return null;
    }

    private PaymentJournalRowDto mapRow(
            Payment payment,
            Currency referenceCurrency,
            Map<UUID, Integer> cycleOrderById
    ) {
        String referenceCurrencyCode = referenceCurrency.getCode();
        AcademicCycle academicCycle = resolveReportCycle(payment);
        String cycleName = academicCycle != null && academicCycle.getName() != null
                ? academicCycle.getName()
                : "-";
        int cycleDisplayOrder = academicCycle != null && academicCycle.getId() != null
                ? cycleOrderById.getOrDefault(academicCycle.getId(), Integer.MAX_VALUE)
                : Integer.MAX_VALUE;

        PaymentJournalRowDto row = new PaymentJournalRowDto();
        row.setCycleName(cycleName);
        row.setCycleDisplayOrder(cycleDisplayOrder);
        row.setClassroomName(classroomNamingService.build(payment.getEnrollment().getClassroom()));
        row.setFeeCategoryName(payment.getAcademicFee().getFeeCategory().getName());
        row.setPaymentDate(payment.getPaymentDate() != null
                ? DATE_FORMATTER.format(payment.getPaymentDate().toLocalDate())
                : (payment.getCreatedAt() != null
                ? DATE_FORMATTER.format(payment.getCreatedAt().toLocalDate())
                : "-"));
        row.setCreatedAtLabel(payment.getCreatedAt() != null
                ? CREATED_AT_FORMATTER.format(payment.getCreatedAt())
                : "-");
        row.setReceiptNumber(payment.getReceiptNumber());
        row.setStudentNumber(defaultValue(payment.getEnrollment().getStudent().getStudentNumber(), "-"));
        row.setStudentFullName(buildStudentFullName(payment.getEnrollment().getStudent()));
        row.setInstallmentLabel(payment.getAcademicFee().getPaymentInstallment() != null
                ? payment.getAcademicFee().getPaymentInstallment().getName()
                : "-");
        row.setPaymentMethod(formatPaymentMethod(payment.getPaymentMethod()));
        row.setCurrencyCode(resolveCurrencyCode(payment.getCurrencyRate()));
        row.setAmountValue(payment.getAmount());
        row.setAmountUsd(toReferenceCurrency(
                payment.getAmount(),
                payment.getCurrencyRate(),
                referenceCurrencyCode
        ));
        row.setAmountLabel(formatAmount(payment.getAmount()));
        row.setCashier("-");
        row.setObservation(defaultValue(payment.getComment(), "-"));
        return row;
    }

    private void enrichGroupMetadata(List<PaymentJournalRowDto> rows, String referenceCurrencyCode) {
        Map<String, List<PaymentJournalRowDto>> byCycle = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getCycleDisplayOrder() + "|" + row.getCycleName(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<PaymentJournalRowDto> cycleRows : byCycle.values()) {
            int cycleCount = cycleRows.size();
            BigDecimal cycleTotalReference = cycleRows.stream()
                    .map(PaymentJournalRowDto::getAmountUsd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String cycleTotalLabel = formatAmount(cycleTotalReference) + " " + referenceCurrencyCode;

            Map<String, List<PaymentJournalRowDto>> byCategory = cycleRows.stream()
                    .collect(Collectors.groupingBy(
                            PaymentJournalRowDto::getFeeCategoryName,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (List<PaymentJournalRowDto> categoryRows : byCategory.values()) {
                int categoryCount = categoryRows.size();
                BigDecimal categoryTotalReference = categoryRows.stream()
                        .map(PaymentJournalRowDto::getAmountUsd)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                String categoryTotalLabel = formatAmount(categoryTotalReference) + " " + referenceCurrencyCode;

                Map<String, List<PaymentJournalRowDto>> byClass = categoryRows.stream()
                        .collect(Collectors.groupingBy(
                                PaymentJournalRowDto::getClassroomName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

                for (List<PaymentJournalRowDto> classRows : byClass.values()) {
                    int classCount = classRows.size();
                    BigDecimal classTotalReference = classRows.stream()
                            .map(PaymentJournalRowDto::getAmountUsd)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    String classTotalLabel = formatAmount(classTotalReference) + " " + referenceCurrencyCode;

                    int rowNumber = 1;
                    for (PaymentJournalRowDto row : classRows) {
                        row.setCyclePaymentCount(cycleCount);
                        row.setCycleTotalLabel(cycleTotalLabel);
                        row.setCategoryPaymentCount(categoryCount);
                        row.setCategoryTotalLabel(categoryTotalLabel);
                        row.setClassPaymentCount(classCount);
                        row.setClassTotalLabel(classTotalLabel);
                        row.setRowNumber(rowNumber++);
                    }
                }
            }
        }
    }

    private int resolveCycleDisplayOrder(Payment payment, Map<UUID, Integer> cycleOrderById) {
        AcademicCycle academicCycle = resolveReportCycle(payment);
        if (academicCycle == null || academicCycle.getId() == null) {
            return Integer.MAX_VALUE;
        }
        return cycleOrderById.getOrDefault(academicCycle.getId(), Integer.MAX_VALUE);
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

    private boolean matchesDateRange(Payment payment, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveDate = null;
        if (payment.getPaymentDate() != null) {
            effectiveDate = payment.getPaymentDate().toLocalDate();
        } else if (payment.getCreatedAt() != null) {
            effectiveDate = payment.getCreatedAt().toLocalDate();
        }
        if (effectiveDate == null) {
            return false;
        }
        return !effectiveDate.isBefore(startDate) && !effectiveDate.isAfter(endDate);
    }

    private Set<UUID> toIdSet(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream().filter(id -> id != null).collect(Collectors.toCollection(HashSet::new));
    }

    private String resolveCycleFilterLabel(Set<UUID> cycleFilter) {
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
        return String.join(", ", names);
    }

    private String buildStudentFullName(Student student) {
        StringBuilder name = new StringBuilder(student.getLastName());
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            name.append(" ").append(student.getMiddleName());
        }
        if (student.getFirstName() != null && !student.getFirstName().isBlank()) {
            name.append(" ").append(student.getFirstName());
        }
        return name.toString().trim();
    }

    private String formatPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "-";
        }
        return switch (paymentMethod) {
            case CASH -> "Espèces";
            case MOBILE_MONEY -> "Mobile Money";
            case BANK_TRANSFER -> "Banque";
            case CHEQUE -> "Chèque";
            case OTHER -> "Autre";
        };
    }

    private Currency resolveAcademicYearCurrency(UUID schoolId) {
        return schoolCurrencyRepository.findActiveDefaultBySchoolId(schoolId)
                .map(schoolCurrency -> schoolCurrency.getCurrency())
                .orElseThrow(() -> new BadRequestException(
                        "Aucune devise par défaut active n'est configurée pour l'école de l'année scolaire."
                ));
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
        if (currencyRate == null || currencyRate.getTargetCurrency() == null || currencyRate.getSourceCurrency() == null) {
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

    private String formatAmount(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount != null ? amount : BigDecimal.ZERO);
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

    private String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
