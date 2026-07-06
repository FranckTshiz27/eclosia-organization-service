package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.PaymentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "report/payment-receipt")
@RequiredArgsConstructor
public class PaymentReportController {

    private final PaymentReportService service;

    @GetMapping
    public ResponseEntity<byte[]> generateByReceiptNumber(@RequestParam String receiptNumber) {
        byte[] pdf = service.generateReceiptByReceiptNumber(receiptNumber);
        return pdfResponse(pdf, "recu-" + receiptNumber + ".pdf");
    }

    @GetMapping("/by-payment/{paymentId}")
    public ResponseEntity<byte[]> generateByPaymentId(@PathVariable UUID paymentId) {
        byte[] pdf = service.generateReceiptByPaymentId(paymentId);
        return pdfResponse(pdf, "recu-paiement-" + paymentId + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
