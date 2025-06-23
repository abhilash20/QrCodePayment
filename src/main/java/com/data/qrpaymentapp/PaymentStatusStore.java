package com.data.qrpaymentapp;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentStatusStore {
    private final Map<String, Boolean> paymentStatusMap = new ConcurrentHashMap<>();

    public void markPaid(String orderId) {
        paymentStatusMap.put(orderId, true);
    }

    public boolean isPaid(String orderId) {
        return paymentStatusMap.getOrDefault(orderId, false);
    }
}
