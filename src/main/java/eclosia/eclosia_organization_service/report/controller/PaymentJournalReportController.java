package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.PaymentJournalReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("report/payment-journal")
@RequiredArgsConstructor
public class PaymentJournalReportController {

    private final PaymentJournalReportService service;

    @GetMapping
    public ResponseEntity<byte[]> generatePaymentJournal(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<UUID> cycleIds,
            @RequestParam(required = false) List<UUID> classroomIds
    ) {
        byte[] pdf = service.generatePaymentJournal(
                schoolId,
                academicYearId,
                startDate,
                endDate,
                cycleIds,
                classroomIds
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("journal-paiements.pdf").build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
