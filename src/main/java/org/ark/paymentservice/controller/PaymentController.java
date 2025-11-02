package org.ark.paymentservice.controller;

import org.ark.paymentservice.dto.PaymentRequest;
import org.ark.paymentservice.dto.PaymentResponse;
import org.ark.paymentservice.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://127.0.0.1:5533")  // Allow your frontend

public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    public PaymentResponse processPayment(@RequestBody PaymentRequest request) {
        return paymentService.processPayment(request);
    }
}
