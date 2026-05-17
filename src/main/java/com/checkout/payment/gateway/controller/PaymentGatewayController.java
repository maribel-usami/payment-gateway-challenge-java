package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.util.CardUtils;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayController.class);

  private static final String SERVICE_NAME = "Payment Gateway";

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    LOG.info("[{}][Get payment by ID][GET /payment/{id}][requestParams: id={}]",
        SERVICE_NAME, id);

    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping("/payment")
  public ResponseEntity<PostPaymentResponse> postPayment(@RequestBody PostPaymentRequest paymentRequest) {
    LOG.info(
        "[{}][Process payment][POST {}][requestParams: {}]",
        SERVICE_NAME,
        "/payment",
        toSanitizedLogParams(paymentRequest));
    PostPaymentResponse response = paymentGatewayService.processPayment(paymentRequest);
    LOG.info("[{}][Process payment SUCCESS][Response: paymentId={}, status={}]",
        SERVICE_NAME, response.getId(), response.getStatus());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  private String toSanitizedLogParams(PostPaymentRequest paymentRequest) {
    if (paymentRequest == null) {
      return "null";
    }
    return "cardNumberLastFour=" + CardUtils.getCardNumberLastFour(paymentRequest.getCardNumber())
        + ", expiryMonth=" + paymentRequest.getExpiryMonth()
        + ", expiryYear=" + paymentRequest.getExpiryYear()
        + ", currency=" + paymentRequest.getCurrency()
        + ", amount=" + paymentRequest.getAmount();
  }
}
