package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.academic_cycle.entity.AcademicCycle;
import eclosia.eclosia_organization_service.academic_cycle.repository.AcademicCycleRepository;
import eclosia.eclosia_organization_service.academic_fee.entity.AcademicFee;
import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.report.dto.ConfiguredFeesRowDto;
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
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
public class ConfiguredFeesReportService {

    private static final String REPORT_PATH = "report/configured_fees.jrxml";
    private static final String ECLOSIA_LOGO_PATH = "report/eclosia-logo.png";
    private static final String UNIQUE_INSTALLMENT_LABEL = "Paiement unique";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AcademicFeeRepository academicFeeRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNamingService classroomNamingService;
    private final SchoolCurrencyRepository schoolCurrencyRepository;

    @Transactional(readOnly = true)
    public byte[] generateConfiguredFeesReport(
            UUID schoolId,
            UUID academicYearId,
            List<UUID> cycleIds,
            List<UUID> classroomIds
    ) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));

        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to provided school");
        }

        Currency referenceCurrency = resolveSchoolCurrency(schoolId);
        Set<UUID> cycleFilter = toIdSet(cycleIds);
        Set<UUID> classroomFilter = toIdSet(classroomIds);
        List<Classroom> classroomsForFilter = resolveClassroomsForFilter(schoolId, classroomFilter);
        Map<UUID, Integer> cycleOrderById = buildCycleOrderIndex();

        List<AcademicFee> fees = academicFeeRepository
                .findBySchoolIdAndAcademicYearIdOrdered(schoolId, academicYearId)
                .stream()
                .filter(fee -> matchesCycleFilter(fee, cycleFilter))
                .filter(fee -> matchesClassroomFilter(fee, classroomsForFilter))
                .toList();

        List<ConfiguredFeesRowDto> rows = buildRows(fees, cycleOrderById);
        Map<String, Object> parameters = buildParameters(
                school,
                academicYear,
                referenceCurrency,
                cycleFilter,
                classroomFilter,
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
            throw new IllegalStateException("Unable to generate configured fees report", exception);
        }
    }

    private List<ConfiguredFeesRowDto> buildRows(List<AcademicFee> fees, Map<UUID, Integer> cycleOrderById) {
        List<ConfiguredFeesRowDto> rows = fees.stream()
                .map(fee -> mapRow(fee, cycleOrderById))
                .sorted(Comparator
                        .comparing(ConfiguredFeesRowDto::getFeeCategoryName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ConfiguredFeesRowDto::getCycleDisplayOrder)
                        .thenComparing(ConfiguredFeesRowDto::getCycleName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ConfiguredFeesRowDto::getInstallmentDisplayOrder)
                        .thenComparing(ConfiguredFeesRowDto::getInstallmentName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ConfiguredFeesRowDto::getApplicableToLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ConfiguredFeesRowDto::getCode, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));

        enrichGroupMetadata(rows);
        return rows;
    }

    private ConfiguredFeesRowDto mapRow(AcademicFee fee, Map<UUID, Integer> cycleOrderById) {
        AcademicCycle cycle = fee.getAcademicCycle();
        String cycleName = cycle != null && cycle.getName() != null ? cycle.getName() : "-";
        int cycleDisplayOrder = cycle != null && cycle.getId() != null
                ? cycleOrderById.getOrDefault(cycle.getId(), Integer.MAX_VALUE)
                : Integer.MAX_VALUE;

        String installmentName = UNIQUE_INSTALLMENT_LABEL;
        int installmentDisplayOrder = -1;
        if (fee.getPaymentInstallment() != null) {
            installmentName = fee.getPaymentInstallment().getName() != null
                    ? fee.getPaymentInstallment().getName()
                    : fee.getPaymentInstallment().getCode();
            installmentDisplayOrder = fee.getPaymentInstallment().getDisplayOrder() != null
                    ? fee.getPaymentInstallment().getDisplayOrder()
                    : Integer.MAX_VALUE;
        }

        BigDecimal amount = fee.getAmount() != null ? fee.getAmount() : BigDecimal.ZERO;
        boolean active = Boolean.TRUE.equals(fee.getActive());
        boolean payableByInstallment = Boolean.TRUE.equals(fee.getPayableByInstallment())
                || fee.getPaymentInstallment() != null;

        ConfiguredFeesRowDto row = new ConfiguredFeesRowDto();
        row.setCode(defaultValue(fee.getCode(), "-"));
        row.setFeeName(defaultValue(fee.getName(), fee.getFeeCategory() != null ? fee.getFeeCategory().getName() : "-"));
        row.setFeeCategoryName(fee.getFeeCategory() != null ? fee.getFeeCategory().getName() : "-");
        row.setApplicableToLabel(buildApplicableToLabel(fee));
        row.setAnnualAmount(amount);
        row.setAnnualAmountLabel(formatAmount(amount));
        row.setInstallmentCount(1);
        row.setPeriodicityLabel(payableByInstallment ? "Par tranche" : "Unique");
        row.setCalculationModeLabel("Par élève");
        row.setActive(active);
        row.setStatusLabel(active ? "Actif" : "Inactif");
        row.setCycleName(cycleName);
        row.setCycleDisplayOrder(cycleDisplayOrder);
        row.setInstallmentName(installmentName);
        row.setInstallmentDisplayOrder(installmentDisplayOrder);
        return row;
    }

    private String buildApplicableToLabel(AcademicFee fee) {
        StringBuilder label = new StringBuilder();
        if (fee.getAcademicLevel() != null) {
            label.append(fee.getAcademicLevel().getName());
            if (fee.getAcademicOption() != null) {
                label.append(" ").append(fee.getAcademicOption().getName());
            } else if (fee.getAcademicSection() != null) {
                label.append(" ").append(fee.getAcademicSection().getName());
            }
        }
        if (fee.getStudentCategory() != null) {
            if (!label.isEmpty()) {
                label.append(" · ");
            }
            label.append(fee.getStudentCategory().getName());
        }
        return label.isEmpty() ? "-" : label.toString();
    }

    private void enrichGroupMetadata(List<ConfiguredFeesRowDto> rows) {
        Map<String, List<ConfiguredFeesRowDto>> byCategory = rows.stream()
                .collect(Collectors.groupingBy(
                        ConfiguredFeesRowDto::getFeeCategoryName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<ConfiguredFeesRowDto> categoryRows : byCategory.values()) {
            int categoryFeeCount = categoryRows.size();
            long categoryInstallmentCount = categoryRows.stream()
                    .map(row -> row.getCycleDisplayOrder() + "|" + row.getCycleName() + "|" + row.getInstallmentDisplayOrder() + "|" + row.getInstallmentName())
                    .distinct()
                    .count();
            String categoryTotalLabel = formatAmount(sumAmounts(categoryRows));

            Map<String, List<ConfiguredFeesRowDto>> byCycle = categoryRows.stream()
                    .collect(Collectors.groupingBy(
                            row -> row.getCycleDisplayOrder() + "|" + row.getCycleName(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (List<ConfiguredFeesRowDto> cycleRows : byCycle.values()) {
                int cycleFeeCount = cycleRows.size();
                String cycleTotalLabel = formatAmount(sumAmounts(cycleRows));

                Map<String, List<ConfiguredFeesRowDto>> byTranche = cycleRows.stream()
                        .collect(Collectors.groupingBy(
                                row -> row.getInstallmentDisplayOrder() + "|" + row.getInstallmentName(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

                for (List<ConfiguredFeesRowDto> trancheRows : byTranche.values()) {
                    int trancheFeeCount = trancheRows.size();
                    String trancheTotalLabel = formatAmount(sumAmounts(trancheRows));

                    int rowNumber = 1;
                    for (ConfiguredFeesRowDto row : trancheRows) {
                        row.setRowNumber(rowNumber++);
                        row.setCategoryFeeCount(categoryFeeCount);
                        row.setCategoryInstallmentCount((int) categoryInstallmentCount);
                        row.setCategoryTotalAmountLabel(categoryTotalLabel);
                        row.setCycleFeeCount(cycleFeeCount);
                        row.setCycleTotalAmountLabel(cycleTotalLabel);
                        row.setTrancheFeeCount(trancheFeeCount);
                        row.setTrancheTotalAmountLabel(trancheTotalLabel);
                    }
                }
            }
        }
    }

    private BigDecimal sumAmounts(List<ConfiguredFeesRowDto> rows) {
        return rows.stream()
                .map(ConfiguredFeesRowDto::getAnnualAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> buildParameters(
            School school,
            AcademicYear academicYear,
            Currency referenceCurrency,
            Set<UUID> cycleFilter,
            Set<UUID> classroomFilter,
            List<ConfiguredFeesRowDto> rows
    ) {
        String currencyCode = referenceCurrency.getCode();
        int totalFees = rows.size();
        long totalInstallments = rows.stream()
                .map(row -> row.getFeeCategoryName()
                        + "|" + row.getCycleDisplayOrder()
                        + "|" + row.getCycleName()
                        + "|" + row.getInstallmentDisplayOrder()
                        + "|" + row.getInstallmentName())
                .distinct()
                .count();
        long totalCategories = rows.stream()
                .map(ConfiguredFeesRowDto::getFeeCategoryName)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        BigDecimal totalAnnualAmount = sumAmounts(rows);

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
        parameters.put("CURRENCY_FILTER_LABEL", currencyCode);
        parameters.put("REFERENCE_CURRENCY_CODE", currencyCode);
        parameters.put("PRINT_DATETIME", DATETIME_FORMATTER.format(LocalDateTime.now()));
        parameters.put("TOTAL_FEES_COUNT", totalFees);
        parameters.put("TOTAL_ANNUAL_AMOUNT_LABEL", formatAmount(totalAnnualAmount) + " " + currencyCode);
        parameters.put("TOTAL_INSTALLMENTS_COUNT", (int) totalInstallments);
        parameters.put("TOTAL_CATEGORIES_COUNT", (int) totalCategories);
        parameters.put("CURRENCY_NOTE", "NB : Les montants sont exprimés en " + currencyCode + ".");
        return parameters;
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

    private String buildPeriodLabel(AcademicYear academicYear) {
        if (academicYear.getStartDate() == null || academicYear.getEndDate() == null) {
            return "-";
        }
        return DATE_FORMATTER.format(academicYear.getStartDate())
                + " au "
                + DATE_FORMATTER.format(academicYear.getEndDate());
    }

    private boolean matchesCycleFilter(AcademicFee fee, Set<UUID> cycleFilter) {
        if (cycleFilter.isEmpty()) {
            return true;
        }
        return fee.getAcademicCycle() != null
                && fee.getAcademicCycle().getId() != null
                && cycleFilter.contains(fee.getAcademicCycle().getId());
    }

    private boolean matchesClassroomFilter(AcademicFee fee, List<Classroom> classroomsForFilter) {
        if (classroomsForFilter.isEmpty()) {
            return true;
        }
        return classroomsForFilter.stream().anyMatch(classroom -> matchesClassroom(fee, classroom));
    }

    private boolean matchesClassroom(AcademicFee fee, Classroom classroom) {
        if (classroom.getAcademicLevel() == null
                || fee.getAcademicLevel() == null
                || !Objects.equals(fee.getAcademicLevel().getId(), classroom.getAcademicLevel().getId())) {
            return false;
        }
        UUID feeSectionId = fee.getAcademicSection() != null ? fee.getAcademicSection().getId() : null;
        UUID classSectionId = classroom.getAcademicSection() != null ? classroom.getAcademicSection().getId() : null;
        if (!Objects.equals(feeSectionId, classSectionId)) {
            return false;
        }
        UUID feeOptionId = fee.getAcademicOption() != null ? fee.getAcademicOption().getId() : null;
        UUID classOptionId = classroom.getAcademicOption() != null ? classroom.getAcademicOption().getId() : null;
        return Objects.equals(feeOptionId, classOptionId);
    }

    private List<Classroom> resolveClassroomsForFilter(UUID schoolId, Set<UUID> classroomFilter) {
        if (classroomFilter.isEmpty()) {
            return List.of();
        }
        return classroomRepository.findBySchoolIdWithLevelAndCycle(schoolId).stream()
                .filter(classroom -> classroomFilter.contains(classroom.getId()))
                .toList();
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
