package org.ark.paymentservice.service;

import org.ark.paymentservice.dto.PaymentRequest;
import org.ark.paymentservice.dto.PaymentResponse;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public PaymentResponse processPayment(PaymentRequest request) {
        // Simulate payment logic
        if (request.getAmount() > 0) {
            return new PaymentResponse(
                    request.getTransactionId(),
                    "SUCCESS",
                    "Payment processed successfully"
            );
        } else {
            return new PaymentResponse(
                    request.getTransactionId(),
                    "FAILED",
                    "Invalid payment amount"
            );
        }
    }
}

