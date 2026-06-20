package tech.gymsaas.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.gymsaas.backend.dto.payment.PaymentRequest;
import tech.gymsaas.backend.dto.payment.PaymentResponse;
import tech.gymsaas.backend.service.PaymentService;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody PaymentRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createManualInvoice(request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getAllPayments(authentication));
    }
}