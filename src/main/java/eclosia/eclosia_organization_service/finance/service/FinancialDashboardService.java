package eclosia.eclosia_organization_service.finance.service;

import eclosia.eclosia_organization_service.academic_fee.repository.AcademicFeeRepository;
import eclosia.eclosia_organization_service.academic_year.entity.AcademicYear;
import eclosia.eclosia_organization_service.academic_year.repository.AcademicYearRepository;
import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.classroom.repository.ClassroomRepository;
import eclosia.eclosia_organization_service.classroom.service.ClassroomNamingService;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.enrollment.repository.EnrollmentRepository;
import eclosia.eclosia_organization_service.finance.dto.ArrearsSummaryDto;
import eclosia.eclosia_organization_service.finance.dto.ClassPerformanceDto;
import eclosia.eclosia_organization_service.finance.dto.FinancialDashboardDto;
import eclosia.eclosia_organization_service.finance.dto.FinancialDashboardSummaryDto;
import eclosia.eclosia_organization_service.finance.dto.QuickSummaryDto;
import eclosia.eclosia_organization_service.finance.dto.RecentPaymentDto;
import eclosia.eclosia_organization_service.finance.dto.RevenueByCategoryDto;
import eclosia.eclosia_organization_service.finance.dto.RevenueByPaymentMethodDto;
import eclosia.eclosia_organization_service.finance.dto.RevenueEvolutionItemDto;
import eclosia.eclosia_organization_service.finance.projection.CompletedPaymentProjection;
import eclosia.eclosia_organization_service.finance.projection.EnrollmentExpectedProjection;
import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import eclosia.eclosia_organization_service.school.repository.SchoolRepository;
import eclosia.eclosia_organization_service.school_currency.repository.SchoolCurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
public class FinancialDashboardService {

    private static final BigDecimal EPSILON = new BigDecimal("0.0001");
    private static final int TOP_CLASSES = 5;
    private static final int RECENT_PAYMENTS_LIMIT = 5;
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

    private final SchoolRepository schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SchoolCurrencyRepository schoolCurrencyRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final AcademicFeeRepository academicFeeRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomNamingService classroomNamingService;

    @Transactional(readOnly = true)
    public FinancialDashboardDto getDashboard(UUID schoolId, UUID academicYearId) {
        if (schoolId == null || academicYearId == null) {
            throw new BadRequestException("schoolId et academicYearId sont obligatoires");
        }
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResourceNotFoundException("School not found");
        }
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (!schoolId.equals(academicYear.getSchoolId())) {
            throw new BadRequestException("Academic year does not belong to provided school");
        }

        Currency referenceCurrency = schoolCurrencyRepository.findActiveDefaultBySchoolId(schoolId)
                .map(schoolCurrency -> schoolCurrency.getCurrency())
                .orElseThrow(() -> new BadRequestException(
                        "Aucune devise par défaut active n'est configurée pour l'école."
                ));
        String referenceCurrencyCode = referenceCurrency.getCode();

        List<EnrollmentExpectedProjection> expectedRows =
                enrollmentRepository.sumExpectedAmountsByEnrollment(schoolId, academicYearId);
        List<CompletedPaymentProjection> payments =
                paymentRepository.findCompletedPaymentsForDashboard(schoolId, academicYearId);

        Map<UUID, BigDecimal> expectedByEnrollment = new HashMap<>();
        Map<UUID, BigDecimal> expectedByClassroom = new HashMap<>();
        Map<UUID, Set<UUID>> studentsByClassroom = new HashMap<>();
        BigDecimal totalExpected = BigDecimal.ZERO;

        for (EnrollmentExpectedProjection row : expectedRows) {
            if (row.getEnrollmentId() == null) {
                continue;
            }
            BigDecimal expected = safeAmount(row.getExpectedAmount());
            expectedByEnrollment.put(row.getEnrollmentId(), expected);
            totalExpected = totalExpected.add(expected);
            if (row.getClassroomId() != null) {
                expectedByClassroom.merge(row.getClassroomId(), expected, BigDecimal::add);
                studentsByClassroom
                        .computeIfAbsent(row.getClassroomId(), key -> new HashSet<>())
                        .add(row.getEnrollmentId());
            }
        }

        Map<UUID, BigDecimal> collectedByEnrollment = new HashMap<>();
        Map<UUID, BigDecimal> collectedByClassroom = new HashMap<>();
        Map<UUID, CategoryAgg> collectedByCategory = new LinkedHashMap<>();
        Map<PaymentMethod, BigDecimal> collectedByMethod = new EnumMap<>(PaymentMethod.class);
        Map<YearMonth, BigDecimal> collectedByMonth = new LinkedHashMap<>();
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal todayCollected = BigDecimal.ZERO;
        Set<String> todayReceipts = new HashSet<>();
        LocalDate today = LocalDate.now();

        for (PaymentMethod method : PaymentMethod.values()) {
            collectedByMethod.put(method, BigDecimal.ZERO);
        }

        for (CompletedPaymentProjection payment : payments) {
            BigDecimal collected = toReferenceCurrency(
                    payment.getAmount(),
                    payment.getTargetCurrencyCode(),
                    payment.getSourceCurrencyCode(),
                    payment.getExchangeRate(),
                    referenceCurrencyCode
            );
            totalCollected = totalCollected.add(collected);

            if (payment.getEnrollmentId() != null) {
                collectedByEnrollment.merge(payment.getEnrollmentId(), collected, BigDecimal::add);
            }
            if (payment.getClassroomId() != null) {
                collectedByClassroom.merge(payment.getClassroomId(), collected, BigDecimal::add);
            }
            if (payment.getFeeCategoryId() != null) {
                collectedByCategory
                        .computeIfAbsent(payment.getFeeCategoryId(), id -> new CategoryAgg(
                                id,
                                payment.getFeeCategoryCode(),
                                payment.getFeeCategoryName()
                        ))
                        .add(collected);
            }
            if (payment.getPaymentMethod() != null) {
                collectedByMethod.merge(payment.getPaymentMethod(), collected, BigDecimal::add);
            }

            LocalDateTime effectiveDateTime = payment.getPaymentDate() != null
                    ? payment.getPaymentDate()
                    : payment.getCreatedAt();
            if (effectiveDateTime != null) {
                YearMonth yearMonth = YearMonth.from(effectiveDateTime);
                collectedByMonth.merge(yearMonth, collected, BigDecimal::add);
                if (effectiveDateTime.toLocalDate().equals(today)) {
                    todayCollected = todayCollected.add(collected);
                    if (payment.getReceiptNumber() != null) {
                        todayReceipts.add(payment.getReceiptNumber());
                    }
                }
            }
        }

        BigDecimal remainingAmount = totalExpected.subtract(totalCollected).max(BigDecimal.ZERO);
        BigDecimal recoveryRate = percentage(totalCollected, totalExpected);

        long unpaidStudentCount = 0L;
        for (Map.Entry<UUID, BigDecimal> entry : expectedByEnrollment.entrySet()) {
            BigDecimal expected = entry.getValue();
            BigDecimal collected = collectedByEnrollment.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (expected.subtract(collected).compareTo(EPSILON) > 0) {
                unpaidStudentCount++;
            }
        }

        long totalStudents = enrollmentRepository.countActiveBySchoolAndAcademicYear(schoolId, academicYearId);
        long totalClasses = enrollmentRepository.countDistinctActiveClassroomsBySchoolAndAcademicYear(
                schoolId,
                academicYearId
        );

        Map<UUID, String> classroomNames = resolveClassroomNames(
                studentsByClassroom.keySet(),
                collectedByClassroom.keySet()
        );

        List<RecentPaymentDto> recentPayments = buildRecentPayments(
                schoolId,
                academicYearId,
                classroomNames,
                referenceCurrencyCode
        );

        return FinancialDashboardDto.builder()
                .summary(FinancialDashboardSummaryDto.builder()
                        .expectedAmount(scale(totalExpected))
                        .collectedAmount(scale(totalCollected))
                        .remainingAmount(scale(remainingAmount))
                        .recoveryRate(recoveryRate)
                        .totalStudents(totalStudents)
                        .totalClasses(totalClasses)
                        .todayCollectedAmount(scale(todayCollected))
                        .todayPaymentCount(todayReceipts.size())
                        .build())
                .revenueEvolution(buildRevenueEvolution(academicYear, collectedByMonth))
                .revenueByCategory(buildRevenueByCategory(collectedByCategory, totalCollected))
                .classPerformance(buildClassPerformance(
                        expectedByClassroom,
                        collectedByClassroom,
                        studentsByClassroom,
                        classroomNames
                ))
                .revenueByPaymentMethod(buildRevenueByPaymentMethod(collectedByMethod, totalCollected))
                .arrearsSummary(ArrearsSummaryDto.builder()
                        .unpaidStudentCount(unpaidStudentCount)
                        .remainingAmount(scale(remainingAmount))
                        .remainingPercentage(percentage(remainingAmount, totalExpected))
                        .recoveryRate(recoveryRate)
                        .build())
                .recentPayments(recentPayments)
                .quickSummary(buildQuickSummary(
                        schoolId,
                        academicYearId,
                        totalExpected,
                        totalCollected,
                        totalStudents
                ))
                .build();
    }

    private List<RevenueEvolutionItemDto> buildRevenueEvolution(
            AcademicYear academicYear,
            Map<YearMonth, BigDecimal> collectedByMonth
    ) {
        List<RevenueEvolutionItemDto> items = new ArrayList<>();
        if (academicYear.getStartDate() == null || academicYear.getEndDate() == null) {
            return items;
        }
        YearMonth cursor = YearMonth.from(academicYear.getStartDate());
        YearMonth end = YearMonth.from(academicYear.getEndDate());
        while (!cursor.isAfter(end)) {
            BigDecimal amount = collectedByMonth.getOrDefault(cursor, BigDecimal.ZERO);
            items.add(RevenueEvolutionItemDto.builder()
                    .period(formatMonthLabel(cursor))
                    .collectedAmount(scale(amount))
                    .build());
            cursor = cursor.plusMonths(1);
        }
        return items;
    }

    private List<RevenueByCategoryDto> buildRevenueByCategory(
            Map<UUID, CategoryAgg> collectedByCategory,
            BigDecimal totalCollected
    ) {
        return collectedByCategory.values().stream()
                .sorted(Comparator.comparing(CategoryAgg::collectedAmount).reversed())
                .map(agg -> RevenueByCategoryDto.builder()
                        .categoryId(agg.categoryId())
                        .categoryCode(agg.categoryCode())
                        .categoryName(agg.categoryName())
                        .collectedAmount(scale(agg.collectedAmount()))
                        .percentage(percentage(agg.collectedAmount(), totalCollected))
                        .build())
                .toList();
    }

    private List<ClassPerformanceDto> buildClassPerformance(
            Map<UUID, BigDecimal> expectedByClassroom,
            Map<UUID, BigDecimal> collectedByClassroom,
            Map<UUID, Set<UUID>> studentsByClassroom,
            Map<UUID, String> classroomNames
    ) {
        Set<UUID> classroomIds = new HashSet<>();
        classroomIds.addAll(expectedByClassroom.keySet());
        classroomIds.addAll(collectedByClassroom.keySet());

        return classroomIds.stream()
                .map(classroomId -> {
                    BigDecimal expected = expectedByClassroom.getOrDefault(classroomId, BigDecimal.ZERO);
                    BigDecimal collected = collectedByClassroom.getOrDefault(classroomId, BigDecimal.ZERO);
                    BigDecimal remaining = expected.subtract(collected).max(BigDecimal.ZERO);
                    return ClassPerformanceDto.builder()
                            .classroomId(classroomId)
                            .classroomName(classroomNames.getOrDefault(classroomId, "-"))
                            .studentCount(studentsByClassroom.getOrDefault(classroomId, Set.of()).size())
                            .expectedAmount(scale(expected))
                            .collectedAmount(scale(collected))
                            .remainingAmount(scale(remaining))
                            .recoveryRate(percentage(collected, expected))
                            .build();
                })
                .sorted(Comparator
                        .comparing(ClassPerformanceDto::getRecoveryRate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ClassPerformanceDto::getClassroomName, String.CASE_INSENSITIVE_ORDER))
                .limit(TOP_CLASSES)
                .toList();
    }

    private List<RevenueByPaymentMethodDto> buildRevenueByPaymentMethod(
            Map<PaymentMethod, BigDecimal> collectedByMethod,
            BigDecimal totalCollected
    ) {
        return List.of(PaymentMethod.CASH, PaymentMethod.MOBILE_MONEY, PaymentMethod.BANK_TRANSFER).stream()
                .map(method -> {
                    BigDecimal amount = collectedByMethod.getOrDefault(method, BigDecimal.ZERO);
                    return RevenueByPaymentMethodDto.builder()
                            .paymentMethod(formatPaymentMethod(method))
                            .collectedAmount(scale(amount))
                            .percentage(percentage(amount, totalCollected))
                            .build();
                })
                .toList();
    }

    private List<RecentPaymentDto> buildRecentPayments(
            UUID schoolId,
            UUID academicYearId,
            Map<UUID, String> classroomNames,
            String referenceCurrencyCode
    ) {
        List<CompletedPaymentProjection> recentCandidates = paymentRepository
                .findRecentCompletedPaymentsForDashboard(
                        schoolId,
                        academicYearId,
                        PageRequest.of(0, 50)
                );

        Set<UUID> recentClassroomIds = recentCandidates.stream()
                .map(CompletedPaymentProjection::getClassroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<UUID, String> names = new HashMap<>(classroomNames);
        names.putAll(loadClassroomNames(recentClassroomIds));

        Map<String, List<CompletedPaymentProjection>> byReceipt = new LinkedHashMap<>();
        for (CompletedPaymentProjection payment : recentCandidates) {
            String receipt = payment.getReceiptNumber() != null
                    ? payment.getReceiptNumber()
                    : String.valueOf(payment.getPaymentId());
            byReceipt.computeIfAbsent(receipt, key -> new ArrayList<>()).add(payment);
        }

        List<RecentPaymentDto> result = new ArrayList<>();
        for (Map.Entry<String, List<CompletedPaymentProjection>> entry : byReceipt.entrySet()) {
            if (result.size() >= RECENT_PAYMENTS_LIMIT) {
                break;
            }
            List<CompletedPaymentProjection> lines = entry.getValue();
            CompletedPaymentProjection first = lines.getFirst();
            BigDecimal total = BigDecimal.ZERO;
            for (CompletedPaymentProjection line : lines) {
                total = total.add(toReferenceCurrency(
                        line.getAmount(),
                        line.getTargetCurrencyCode(),
                        line.getSourceCurrencyCode(),
                        line.getExchangeRate(),
                        referenceCurrencyCode
                ));
            }
            String currencyCode = first.getTargetCurrencyCode() != null
                    ? first.getTargetCurrencyCode()
                    : referenceCurrencyCode;
            UUID classroomId = first.getClassroomId();
            result.add(RecentPaymentDto.builder()
                    .paymentId(first.getPaymentId())
                    .receiptNumber(first.getReceiptNumber())
                    .paymentDate(first.getPaymentDate())
                    .studentId(first.getStudentId())
                    .studentNumber(defaultValue(first.getStudentNumber(), "-"))
                    .studentFullName(buildStudentFullName(
                            first.getStudentLastName(),
                            first.getStudentMiddleName(),
                            first.getStudentFirstName()
                    ))
                    .classroomId(classroomId)
                    .classroomName(classroomId != null
                            ? names.getOrDefault(classroomId, resolveClassroomName(classroomId))
                            : "-")
                    .amount(scale(total))
                    .currencyCode(currencyCode)
                    .paymentMethod(formatPaymentMethod(first.getPaymentMethod()))
                    .build());
        }
        return result;
    }

    private QuickSummaryDto buildQuickSummary(
            UUID schoolId,
            UUID academicYearId,
            BigDecimal totalExpected,
            BigDecimal totalCollected,
            long totalStudents
    ) {
        long configuredFees = academicFeeRepository.countBySchoolIdAndAcademicYearId(schoolId, academicYearId);
        long configuredInstallments = academicFeeRepository
                .countDistinctInstallmentsBySchoolIdAndAcademicYearId(schoolId, academicYearId);
        BigDecimal divisor = totalStudents > 0
                ? BigDecimal.valueOf(totalStudents)
                : BigDecimal.ONE;
        BigDecimal averageExpected = totalStudents > 0
                ? totalExpected.divide(divisor, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageCollected = totalStudents > 0
                ? totalCollected.divide(divisor, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return QuickSummaryDto.builder()
                .configuredFeesCount(configuredFees)
                .configuredInstallmentsCount(configuredInstallments)
                .averageExpectedPerStudent(averageExpected)
                .averageCollectedPerStudent(averageCollected)
                .build();
    }

    private Map<UUID, String> resolveClassroomNames(Set<UUID> first, Set<UUID> second) {
        Set<UUID> ids = new HashSet<>();
        if (first != null) {
            ids.addAll(first);
        }
        if (second != null) {
            ids.addAll(second);
        }
        return loadClassroomNames(ids);
    }

    private Map<UUID, String> loadClassroomNames(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        return classroomRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        Classroom::getId,
                        classroomNamingService::build,
                        (left, right) -> left
                ));
    }

    private String resolveClassroomName(UUID classroomId) {
        return classroomRepository.findById(classroomId)
                .map(classroomNamingService::build)
                .orElse("-");
    }

    private BigDecimal toReferenceCurrency(
            BigDecimal amount,
            String targetCurrencyCode,
            String sourceCurrencyCode,
            BigDecimal exchangeRate,
            String referenceCurrencyCode
    ) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (targetCurrencyCode == null || sourceCurrencyCode == null) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (referenceCurrencyCode.equalsIgnoreCase(targetCurrencyCode)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (referenceCurrencyCode.equalsIgnoreCase(sourceCurrencyCode)
                && exchangeRate != null
                && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            return amount.divide(exchangeRate, 2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal safePart = part != null ? part : BigDecimal.ZERO;
        return safePart.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal amount) {
        return safeAmount(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private String formatMonthLabel(YearMonth yearMonth) {
        String label = MONTH_FORMATTER.format(yearMonth.atDay(1));
        if (label.isEmpty()) {
            return "-";
        }
        String capitalized = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        return capitalized.endsWith(".") ? capitalized : capitalized + ".";
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

    private String buildStudentFullName(String lastName, String middleName, String firstName) {
        StringBuilder name = new StringBuilder(defaultValue(lastName, ""));
        if (middleName != null && !middleName.isBlank()) {
            name.append(" ").append(middleName.trim());
        }
        if (firstName != null && !firstName.isBlank()) {
            name.append(" ").append(firstName.trim());
        }
        String fullName = name.toString().trim();
        return fullName.isBlank() ? "-" : fullName;
    }

    private String defaultValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static final class CategoryAgg {
        private final UUID categoryId;
        private final String categoryCode;
        private final String categoryName;
        private BigDecimal collectedAmount = BigDecimal.ZERO;

        private CategoryAgg(UUID categoryId, String categoryCode, String categoryName) {
            this.categoryId = categoryId;
            this.categoryCode = categoryCode;
            this.categoryName = categoryName;
        }

        private void add(BigDecimal amount) {
            collectedAmount = collectedAmount.add(amount != null ? amount : BigDecimal.ZERO);
        }

        private UUID categoryId() {
            return categoryId;
        }

        private String categoryCode() {
            return categoryCode;
        }

        private String categoryName() {
            return categoryName;
        }

        private BigDecimal collectedAmount() {
            return collectedAmount;
        }
    }
}
