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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RazorpayService razorpayService;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @GetMapping("/contributors")
    public String contributors(Model model) {
        List<Transaction> contributors = transactionRepository.findAllByOrderByTimestampDesc();
        model.addAttribute("contributors", contributors);
        model.addAttribute("totalContributors", contributors.size());
        return "contributors";
    }

    @PostMapping("/api/create-order")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody Map<String, String> data) {
        try {
            String contributorName = data.get("name");
            Order order = razorpayService.createOrder(contributorName);
            Map<String, String> response = new HashMap<>();

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
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody Map<String, String> paymentDetails) {
        try {
            String orderId = paymentDetails.get("razorpay_order_id");
            String paymentId = paymentDetails.get("razorpay_payment_id");
            String signature = paymentDetails.get("razorpay_signature");
            String contributorName = paymentDetails.get("contributor_name");

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            Map<String, String> response = new HashMap<>();
            if (isValid) {
                Transaction transaction = new Transaction(paymentId, contributorName, "captured", 1L, LocalDateTime.now());
                transactionRepository.save(transaction);
                
                long totalContributors = transactionRepository.countByStatus("captured");

                response.put("status", "success");
                response.put("name", contributorName);
                response.put("rank", String.valueOf(totalContributors));
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
