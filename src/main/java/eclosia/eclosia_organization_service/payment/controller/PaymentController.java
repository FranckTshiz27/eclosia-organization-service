package eclosia.eclosia_organization_service.payment.controller;

import eclosia.eclosia_organization_service.payment.dto.CreatePaymentBatch;
import eclosia.eclosia_organization_service.payment.dto.UpdatePaymentDto;
import eclosia.eclosia_organization_service.payment.entity.Payment;
import eclosia.eclosia_organization_service.payment.entity.PaymentStatus;
import eclosia.eclosia_organization_service.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "payment")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService service;

    @PostMapping
    public ResponseEntity<List<Payment>> create(
            @NotEmpty(message = "At least one payment is required")
            @Valid @RequestBody CreatePaymentBatch dtos
    ) {
        List<Payment> payments = service.createAll(dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(payments);
    }

    @GetMapping
    public List<Payment> findAll(
            @RequestParam(required = false) UUID enrollmentId,
            @RequestParam(required = false) UUID academicFeeId,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) PaymentStatus status
    ) {
        return service.findAll(enrollmentId, academicFeeId, schoolId, status);
    }

    @GetMapping("/{id}")
    public Payment findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Payment update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
