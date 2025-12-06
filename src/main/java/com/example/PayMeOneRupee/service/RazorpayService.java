package com.example.PayMeOneRupee.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public Order createOrder() throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", 100); // amount in the smallest currency unit (100 paise = ₹1)
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_12345"); // A unique receipt ID for your reference

        return razorpayClient.orders.create(orderRequest);
    }
}
