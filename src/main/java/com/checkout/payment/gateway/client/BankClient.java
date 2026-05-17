package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Send request to bank service
 */
@Component
public class BankClient {

  private final RestTemplate restTemplate;
  private final String bankPaymentsUrl;

  public BankClient(RestTemplate restTemplate,
                    @Value("${bank.payments-url}") String bankPaymentsUrl) {
    this.restTemplate = restTemplate;
    this.bankPaymentsUrl = bankPaymentsUrl;
  }

  public BankPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    try {
      return restTemplate.postForObject(
          bankPaymentsUrl,
          new BankPaymentRequest(paymentRequest),
          BankPaymentResponse.class);
    } catch (RestClientException ex) {
      return null;
    }
  }
}
