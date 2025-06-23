package com.data.qrpaymentapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.codec.binary.Hex;
import java.nio.charset.StandardCharsets;

@RestController
public class WebhookController {

    @Value("${authorizenet.webhooksignaturekey}")
    private String signatureKey;

    private final PaymentStatusStore paymentStatusStore;

    public WebhookController(PaymentStatusStore paymentStatusStore) {
        this.paymentStatusStore = paymentStatusStore;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestHeader("X-ANET-SIGNATURE") String sig, @RequestBody byte[] rawBody) throws Exception {
        String payload = new String(rawBody, StandardCharsets.UTF_8);
        String computed = "sha512=" + HmacUtils.hmacSha512Hex(signatureKey.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        System.out.println("Signature from header: " + sig);
        System.out.println("Computed signature: " + computed);
        System.out.println("Payload: " + payload);

        if (!computed.equalsIgnoreCase(sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        JsonNode node = new ObjectMapper().readTree(payload);


        if ("net.authorize.payment.authcapture.created".equals(node.get("eventType").asText())) {
            String transId = node.at("/payload/id").asText();
            String orderId = node.at("/payload/invoiceNumber").asText();
            System.out.println("Webhook Payload JSON: " + node.toPrettyString());
            paymentStatusStore.markPaid(orderId); // ✅ mark order as paid

            return ResponseEntity.ok("Payment " + transId + " confirmed for order " + orderId);
        }

        return ResponseEntity.ok("Ignored event");
    }
}
