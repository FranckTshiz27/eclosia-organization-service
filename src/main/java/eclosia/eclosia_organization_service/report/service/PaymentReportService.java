package eclosia.eclosia_organization_service.report.service;

import eclosia.eclosia_organization_service.classroom.entity.Classroom;
import eclosia.eclosia_organization_service.common.exception.BadRequestException;
import eclosia.eclosia_organization_service.common.exception.ResourceNotFoundException;
import eclosia.eclosia_organization_service.currency.entity.Currency;
import eclosia.eclosia_organization_service.currency_rate.entity.CurrencyRate;
import eclosia.eclosia_organization_service.enrollment.entity.Enrollment;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentMethod;
import eclosia.eclosia_organization_service.payment.repository.PaymentRepository;
import eclosia.eclosia_organization_service.report.dto.PaymentReceiptRowDto;
import eclosia.eclosia_organization_service.report.util.FrenchAmountInWordsConverter;
import eclosia.eclosia_organization_service.school.entity.School;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentReportService {

    private static final String REPORT_PATH = "report/payment_receipt.jrxml";
    private static final String ECLOSIA_LOGO_PATH = "report/eclosia-logo.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] generateReceiptByPaymentId(UUID paymentId) {
        Payment payment = paymentRepository.findByIdWithRelations(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getReceiptNumber() == null || payment.getReceiptNumber().isBlank()) {
            throw new BadRequestException("Ce paiement n'a pas de numéro de reçu.");
        }

        return generateReceiptByReceiptNumber(payment.getReceiptNumber());
    }

    @Transactional(readOnly = true)
    public byte[] generateReceiptByReceiptNumber(String receiptNumber) {
        String trimmedReceiptNumber = receiptNumber != null ? receiptNumber.trim() : "";
        if (trimmedReceiptNumber.isEmpty()) {
            throw new BadRequestException("Le numéro de reçu est obligatoire.");
        }

        List<Payment> payments = paymentRepository.findByReceiptNumberWithDetails(trimmedReceiptNumber);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Aucun paiement trouvé pour ce numéro de reçu.");
        }

        Payment firstPayment = payments.getFirst();
        Enrollment enrollment = firstPayment.getEnrollment();
        School school = enrollment.getClassroom().getSchool();
        CurrencyRate currencyRate = firstPayment.getCurrencyRate();

        List<PaymentReceiptRowDto> rows = mapRows(payments);
        BigDecimal totalFees = payments.stream()
                .map(payment -> payment.getAcademicFee().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalFees.subtract(totalPaid);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime paymentDate = firstPayment.getPaymentDate() != null
                ? firstPayment.getPaymentDate()
                : now;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LOGO_IMAGE", resolveLogoImage(school));
        parameters.put("SCHOOL_NAME", resolveSchoolName(school));
        parameters.put("SCHOOL_MOTTO", defaultValue(school.getMotto(), "Éduquer, Encadrer, Épanouir"));
        parameters.put("SCHOOL_ADDRESS", defaultValue(school.getAddress(), "-"));
        parameters.put("SCHOOL_PHONE", defaultValue(school.getPhone(), "-"));
        parameters.put("SCHOOL_EMAIL", defaultValue(school.getEmail(), "-"));
        parameters.put("SCHOOL_WEBSITE", defaultValue(school.getWebsite(), "-"));
        parameters.put("RECEIPT_NUMBER", trimmedReceiptNumber);
        parameters.put("RECEIPT_BARCODE", trimmedReceiptNumber.replace("-", ""));
        parameters.put("ISSUE_DATE", DATE_FORMATTER.format(paymentDate));
        parameters.put("ISSUE_TIME", TIME_FORMATTER.format(paymentDate));
        parameters.put("PRINT_DATETIME", DATETIME_FORMATTER.format(now));
        parameters.put("STUDENT_FULL_NAME", buildStudentFullNameForReceipt(enrollment.getStudent()));
        parameters.put("STUDENT_NUMBER", defaultValue(enrollment.getStudent().getStudentNumber(), "-"));
        parameters.put("CLASSROOM_NAME", buildClassroomName(enrollment.getClassroom()));
        parameters.put("ACADEMIC_YEAR_LABEL", enrollment.getAcademicYear().getCode());
        parameters.put("STUDENT_CATEGORY", resolveStudentCategoryLabel(enrollment));
        parameters.put("GUARDIAN_PHONE", defaultValue(enrollment.getGuardian().getPhoneNumber(), "-"));
        parameters.put("PHOTO_IMAGE", resolvePhotoImage(enrollment));
        parameters.put("PAYMENT_METHOD", formatPaymentMethod(firstPayment.getPaymentMethod()));
        parameters.put("PAYMENT_OPERATOR", formatPaymentOperator(firstPayment.getPaymentMethod()));
        parameters.put("TRANSACTION_REFERENCE", defaultValue(firstPayment.getTransactionReference(), "-"));
        parameters.put("REFERENCE_NUMBER", defaultValue(firstPayment.getReferenceNumber(), "-"));
        parameters.put("SENDER_PHONE", defaultValue(enrollment.getGuardian().getPhoneNumber(), "-"));
        parameters.put("CURRENCY_LABEL", formatCurrencyLabel(currencyRate));
        parameters.put("CURRENCY_CODE", resolveCurrencyCode(currencyRate));
        parameters.put("EXCHANGE_RATE_LABEL", formatExchangeRateLabel(currencyRate));
        parameters.put("PAYMENT_DATE", DATE_FORMATTER.format(paymentDate));
        parameters.put("TOTAL_FEES", formatAmount(totalFees, currencyRate));
        parameters.put("TOTAL_PAID", formatAmount(totalPaid, currencyRate));
        parameters.put("REMAINING_BALANCE", formatAmount(remaining.max(BigDecimal.ZERO), currencyRate));
        parameters.put("TOTAL_PAID_WORDS", buildAmountInWords(totalPaid, currencyRate));
        parameters.put("PREPARED_BY", "-");

        try (InputStream reportStream = new ClassPathResource(REPORT_PATH).getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(reportStream);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    new JRBeanCollectionDataSource(rows)
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException exception) {
            throw new IllegalStateException("Unable to generate payment receipt", exception);
        }
    }

    private List<PaymentReceiptRowDto> mapRows(List<Payment> payments) {
        AtomicInteger counter = new AtomicInteger(1);
        CurrencyRate currencyRate = payments.getFirst().getCurrencyRate();

        return payments.stream().map(payment -> {
            PaymentReceiptRowDto row = new PaymentReceiptRowDto();
            row.setRowNumber(counter.getAndIncrement());
            row.setFeeLabel(payment.getAcademicFee().getFeeCategory() != null
                    ? payment.getAcademicFee().getFeeCategory().getName()
                    : "-");
            row.setPeriodDetail(payment.getAcademicFee().getPaymentInstallment() != null
                    ? payment.getAcademicFee().getPaymentInstallment().getName()
                    : "-");
            row.setUnitAmount(formatAmount(payment.getAcademicFee().getAmount(), currencyRate));
            row.setQuantity(1);
            row.setPaidAmount(formatAmount(payment.getAmount(), currencyRate));
            return row;
        }).collect(Collectors.toList());
    }

    private String buildStudentFullNameForReceipt(Student student) {
        StringBuilder name = new StringBuilder();
        appendNamePart(name, student.getLastName());
        appendNamePart(name, student.getMiddleName());
        appendNamePart(name, student.getFirstName());
        return name.length() > 0 ? name.toString() : "-";
    }

    private void appendNamePart(StringBuilder name, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (name.length() > 0) {
            name.append(' ');
        }
        name.append(part.trim());
    }

    private String buildClassroomName(Classroom classroom) {
        String level = classroom.getAcademicLevel() != null ? classroom.getAcademicLevel().getName() : "";
        String section = classroom.getAcademicSection() != null ? classroom.getAcademicSection().getName() : "";
        String option = classroom.getAcademicOption() != null ? classroom.getAcademicOption().getName() : "";
        String designation = classroom.getClassroomDesignation() != null
                ? classroom.getClassroomDesignation().getName()
                : "";
        return (level + " " + section + " " + option + " " + designation).trim().replaceAll("\\s+", " ");
    }

    private String resolveStudentCategoryLabel(Enrollment enrollment) {
        if (enrollment.getStudentCategory() == null) {
            return "-";
        }
        return defaultValue(enrollment.getStudentCategory().getName(), "-");
    }

    private Object resolvePhotoImage(Enrollment enrollment) {
        if (enrollment.getPhoto() == null) {
            return null;
        }
        File photoFile = Paths.get(enrollment.getPhoto().getPath(), enrollment.getPhoto().getFileName()).toFile();
        if (!photoFile.exists()) {
            return null;
        }
        try {
            return toCircularImageStream(photoFile);
        } catch (IOException exception) {
            return photoFile;
        }
    }

    private InputStream toCircularImageStream(File source) throws IOException {
        BufferedImage original = ImageIO.read(source);
        if (original == null) {
            return new FileInputStream(source);
        }

        int size = Math.min(original.getWidth(), original.getHeight());
        int cropX = (original.getWidth() - size) / 2;
        int cropY = (original.getHeight() - size) / 2;
        BufferedImage square = original.getSubimage(cropX, cropY, size, size);

        BufferedImage circular = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = circular.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setClip(new Ellipse2D.Float(0, 0, size, size));
        graphics.drawImage(square, 0, 0, null);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(circular, "png", outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private String formatPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "-";
        }
        return switch (paymentMethod) {
            case CASH -> "Espèces";
            case MOBILE_MONEY -> "Mobile Money";
            case BANK_TRANSFER -> "Virement bancaire";
            case CHEQUE -> "Chèque";
            case OTHER -> "Autre";
        };
    }

    private String formatPaymentOperator(PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.MOBILE_MONEY) {
            return "Mobile Money";
        }
        return "-";
    }

    private Object resolveLogoImage(School school) {
        if (school.getLogo() != null && !school.getLogo().isBlank()) {
            return Paths.get(school.getLogo()).toFile();
        }
        java.net.URL logoUrl = getClass().getClassLoader().getResource(ECLOSIA_LOGO_PATH);
        if (logoUrl == null) {
            return null;
        }
        return logoUrl;
    }

    private String resolveCurrencyCode(CurrencyRate currencyRate) {
        if (currencyRate != null && currencyRate.getTargetCurrency() != null) {
            return currencyRate.getTargetCurrency().getCode();
        }
        return "FC";
    }

    private String formatCurrencyLabel(CurrencyRate currencyRate) {
        if (currencyRate == null || currencyRate.getTargetCurrency() == null) {
            return "-";
        }
        Currency target = currencyRate.getTargetCurrency();
        return target.getCode() + " - " + target.getName();
    }

    private String formatExchangeRateLabel(CurrencyRate currencyRate) {
        if (currencyRate == null
                || currencyRate.getSourceCurrency() == null
                || currencyRate.getTargetCurrency() == null) {
            return "-";
        }
        return "1 "
                + currencyRate.getSourceCurrency().getCode()
                + " = "
                + formatNumber(currencyRate.getRate())
                + " "
                + currencyRate.getTargetCurrency().getCode();
    }

    private String formatAmount(BigDecimal amount, CurrencyRate currencyRate) {
        String formatted = formatNumber(amount);
        if (currencyRate != null && currencyRate.getTargetCurrency() != null) {
            return formatted + " " + currencyRate.getTargetCurrency().getCode();
        }
        return formatted;
    }

    private String formatNumber(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private String buildAmountInWords(BigDecimal amount, CurrencyRate currencyRate) {
        String currencyName = currencyRate != null && currencyRate.getTargetCurrency() != null
                ? currencyRate.getTargetCurrency().getName()
                : "monnaie locale";
        return FrenchAmountInWordsConverter.convert(amount, currencyName);
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
