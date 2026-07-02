package eclosia.eclosia_organization_service.report.controller;

import eclosia.eclosia_organization_service.report.service.EnrollmentReportService;
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

import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = {"report", "enrollment-report"})
@RequiredArgsConstructor
public class EnrollmentReportController {

    private final EnrollmentReportService service;

    @GetMapping("/students-by-class")
    public ResponseEntity<byte[]> generateStudentsByClass(
            @RequestParam UUID schoolId,
            @RequestParam UUID academicYearId
    ) {
        byte[] pdf = service.generateStudentsByClassReport(schoolId, academicYearId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("liste-eleves-par-classe.pdf").build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
