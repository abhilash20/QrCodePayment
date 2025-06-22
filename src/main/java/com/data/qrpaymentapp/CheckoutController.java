package com.data.qrpaymentapp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import net.authorize.Environment;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.GetHostedPaymentPageController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@RestController
public class CheckoutController {

    @Value("${authorizenet.loginid}") private String loginId;
    @Value("${authorizenet.transactionkey}") private String txnKey;

    @GetMapping("/checkout/{orderId}")
    public ResponseEntity<Void> checkout(@PathVariable String orderId) throws Exception {
        MerchantAuthenticationType auth = new MerchantAuthenticationType();
        auth.setName(loginId); auth.setTransactionKey(txnKey);
        ApiOperationBase.setEnvironment(Environment.SANDBOX);
        ApiOperationBase.setMerchantAuthentication(auth);

        // Build settings and transaction
        TransactionRequestType txn = new TransactionRequestType();
        txn.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txn.setAmount(new BigDecimal("5.00"));
        GetHostedPaymentPageRequest req = new GetHostedPaymentPageRequest();
        req.setMerchantAuthentication(auth);
        req.setTransactionRequest(txn);
        // Check and initialize hostedPaymentSettings if it is null
        if (req.getHostedPaymentSettings() == null) {
            req.setHostedPaymentSettings(new ArrayOfSetting());
        }

        SettingType s = new SettingType();
        s.setSettingName("hostedPaymentReturnOptions");
//        s.setSettingValue("{\"showReceipt\":false,\"url\":\"http://localhost:8080/pay\",\"cancelUrl\":\"http://localhost:8080/pay\"}");
        String returnOptions = "{\"showReceipt\":false,\"url\":\"https://26a8-75-181-241-45.ngrok-free.app/pay\",\"cancelUrl\":\"https://26a8-75-181-241-45.ngrok-free.app/pay\"}";
        s.setSettingValue(returnOptions);

        req.getHostedPaymentSettings().getSetting().add(s);



        GetHostedPaymentPageController ctrl = new GetHostedPaymentPageController(req);
        ctrl.execute();
        GetHostedPaymentPageResponse resp = ctrl.getApiResponse();
//        System.out.println(resp.getMessages().getMessage());
        if (resp != null) {
            String token = resp.getToken();
            System.out.println("Token: " + token);

            // Check if the response contains messages
            if (resp.getMessages() != null && resp.getMessages().getMessage() != null) {
                // Iterate through the list of messages and print each message's text
                resp.getMessages().getMessage().forEach(message -> {
                    System.out.println("Message: " + message.getText()); // Print the message text
                });
            } else {
                System.out.println("No messages in the response.");
            }
        } else {
            System.out.println("Response is null!");
        }

        if (resp == null || resp.getToken() == null) {
            throw new Exception("Failed to retrieve payment token from Authorize.Net");
        }

        String token = resp.getToken();
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString());
//        String payUrl = "https://test.authorize.net/payment/payment?token=" + token;
        String payUrl = "https://26a8-75-181-241-45.ngrok-free.app/pay?token=" + encodedToken;
        System.out.println("Pay URL: " + payUrl);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        BitMatrix matrix = new MultiFormatWriter().encode(payUrl, BarcodeFormat.QR_CODE, 300, 300);
//        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
//
//        HttpHeaders h = new HttpHeaders();
//        h.setContentType(MediaType.IMAGE_PNG);
//        return new ResponseEntity<>(baos.toByteArray(), h, HttpStatus.OK);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, payUrl)
                .build();
    }
}
