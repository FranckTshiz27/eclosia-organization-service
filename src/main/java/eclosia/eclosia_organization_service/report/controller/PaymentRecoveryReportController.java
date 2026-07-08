package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.PaymentRecoveryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "report/payment-recovery")
public class PaymentRecoveryReportController {

    private final PaymentRecoveryReportService service;

    @GetMapping("/dashboard")
    public ResponseEntity<byte[]> generateDashboard(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId,
            /**
             * Liste des tranches (PaymentInstallment) à considérer.
             * Exemple : ?trancheIds=id1&trancheIds=id2
             */
            @RequestParam List<UUID> trancheIds,
            @RequestParam(required = false) List<UUID> cycleIds,
            @RequestParam(required = false) List<UUID> classroomIds
    ) {
        byte[] pdf = service.generatePaymentRecoveryDashboard(
                schoolId,
                academicYearId,
                trancheIds,
                cycleIds,
                classroomIds
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("recovery-dashboard.pdf").build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}

