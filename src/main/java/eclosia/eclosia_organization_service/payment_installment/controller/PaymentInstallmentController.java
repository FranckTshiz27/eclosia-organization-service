package eclosia.eclosia_organization_service.payment_installment.controller;

import eclosia.eclosia_organization_service.payment_installment.dto.CreatePaymentInstallmentDto;
import eclosia.eclosia_organization_service.payment_installment.dto.UpdatePaymentInstallmentDto;
import eclosia.eclosia_organization_service.payment_installment.entity.PaymentInstallment;
import eclosia.eclosia_organization_service.payment_installment.service.PaymentInstallmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping(path = "payment-installment")
@RequiredArgsConstructor
public class PaymentInstallmentController {

    private final PaymentInstallmentService service;

    @PostMapping
    public ResponseEntity<PaymentInstallment> create(@Valid @RequestBody CreatePaymentInstallmentDto dto) {
        PaymentInstallment paymentInstallment = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentInstallment);
    }

    @GetMapping
    public List<PaymentInstallment> findAll(
            @RequestParam(required = false) UUID schoolId
    ) {
        if (schoolId != null) {
            return service.findBySchoolId(schoolId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PaymentInstallment findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public PaymentInstallment update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentInstallmentDto dto
    ) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
