package com.data.qrpaymentapp;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class    PaymentController {

    // STEP 1: Forward the token to Authorize.Net's hosted payment page
//    @GetMapping("/pay")
//    public ResponseEntity<String> forwardToAuthorizeNet(@RequestParam String token) {
//        String html = "<html><body><form id='f' method='POST' action='https://test.authorize.net/payment/payment'>" +
//                "<input type='hidden' name='token' value='" + token + "'/></form>" +
//                "<script>document.getElementById('f').submit();</script></body></html>";
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.TEXT_HTML)
//                .body(html);
//    }
    private final PaymentStatusStore paymentStatusStore;
    public PaymentController(PaymentStatusStore paymentStatusStore) {
        this.paymentStatusStore = paymentStatusStore;
    }

    @GetMapping("/pay")
    public ResponseEntity<String> forwardToAuthorizeNet(@RequestParam String token) {
        String html = "<html><body>" +
                "<form id='f' method='POST' action='https://test.authorize.net/payment/payment'>" +
                "<input type='hidden' name='token' value='" + token + "'/>" +
                "</form>" +
                "<p>Form ready to submit. <button onclick=\"document.getElementById('f').submit();\">Submit</button></p>" +
                "</body></html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }


    // STEP 2: This is called AFTER payment is done
    @GetMapping("/payment/return/{orderId}")
    public ResponseEntity<String> handlePaymentReturn(
            @PathVariable String orderId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String responseCode,
            @RequestParam(required = false) String authCode,
            @RequestParam(required = false) String message
    ) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>Payment Status for Order: ").append(orderId).append("</h2>");

        if ("1".equals(responseCode)) {
            html.append("<p><strong>Payment Successful!</strong></p>");
            html.append("<p>Transaction ID: ").append(transactionId != null ? transactionId : "N/A").append("</p>");
            html.append("<p>Auth Code: ").append(authCode != null ? authCode : "N/A").append("</p>");
        } else {
            html.append("<p><strong>Payment Failed or Cancelled.</strong></p>");
            if (message != null) {
                html.append("<p>Message: ").append(message).append("</p>");
            }
        }

        html.append("</body></html>");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html.toString());
    }

    // Optional: cancel URL handler
    @GetMapping("/payment/cancel/{orderId}")
    public ResponseEntity<String> handleCancel(@PathVariable String orderId) {
        String html = "<html><body><h2>Payment Cancelled</h2><p>Order ID: " + orderId + "</p></body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping("/payment/status/{orderId}")
    public Map<String, Boolean> checkStatus(@PathVariable String orderId) {
        boolean paid = paymentStatusStore.isPaid(orderId);
        return Collections.singletonMap("paid", paid);
    }

}
