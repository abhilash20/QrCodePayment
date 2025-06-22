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

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestHeader("X-ANET-SIGNATURE") String sig, @RequestBody byte[] rawBody) throws Exception {
        // Convert rawBody to string
        String payload = new String(rawBody, StandardCharsets.UTF_8);

        // Compute the HMAC hash using the correct signatureKey and payload as byte[]
        String computed = "sha512=" + HmacUtils.hmacSha512Hex(signatureKey.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));

        // Compare signature
        if (!computed.equals(sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        // Process payload if signature is valid
        JsonNode node = new ObjectMapper().readTree(payload);
        if ("net.authorize.payment.authcapture.created".equals(node.get("eventType").asText())) {
            String transId = node.at("/payload/id").asText();
            // TODO: Mark order as paid in your system
            return ResponseEntity.ok("Payment " + transId + " confirmed");
        }

        return ResponseEntity.ok("Ignored event");
    }
}