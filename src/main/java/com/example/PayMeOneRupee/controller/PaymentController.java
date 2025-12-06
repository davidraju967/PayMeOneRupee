package com.example.PayMeOneRupee.controller;

import com.example.PayMeOneRupee.entity.Transaction;
import com.example.PayMeOneRupee.repository.TransactionRepository;
import com.example.PayMeOneRupee.service.RazorpayService;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RazorpayService razorpayService;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @GetMapping("/api/stats")
    public Map<String, Object> getStats() {
        long totalMembers = transactionRepository.countByStatus("captured");
        long totalAmount = totalMembers; // Since each payment is ₹1
        List<Transaction> recentTransactions = transactionRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMembers", totalMembers);
        stats.put("totalAmount", totalAmount);
        stats.put("recentTransactions", recentTransactions);
        return stats;
    }

    @PostMapping("/api/create-order")
    public ResponseEntity<Map<String, String>> createOrder() {
        try {
            Order order = razorpayService.createOrder();
            Map<String, String> response = new HashMap<>();

            // Explicitly cast the values from the Order object
            String orderId = (String) order.get("id");
            Integer amount = (Integer) order.get("amount");

            response.put("orderId", orderId);
            response.put("amount", amount.toString());
            
            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            logger.error("Error creating Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/api/verify-payment")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody Map<String, String> paymentDetails) {
        try {
            String orderId = paymentDetails.get("razorpay_order_id");
            String paymentId = paymentDetails.get("razorpay_payment_id");
            String signature = paymentDetails.get("razorpay_signature");

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            Map<String, String> response = new HashMap<>();
            if (isValid) {
                // Save the successful transaction
                Transaction transaction = new Transaction(paymentId, "captured", 100L, LocalDateTime.now());
                transactionRepository.save(transaction);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "failure");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error verifying payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
