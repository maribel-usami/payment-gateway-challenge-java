package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Client responsible for sending payment authorization requests to the bank service.
 */
@Component
public class BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);

  private final RestTemplate restTemplate;
  private final String bankPaymentsUrl;

  public BankClient(RestTemplate restTemplate,
                    @Value("${bank.payments-url}") String bankPaymentsUrl) {
    this.restTemplate = restTemplate;
    this.bankPaymentsUrl = bankPaymentsUrl;
  }

  /**
   * Sends a payment request to the configured bank endpoint.
   *
   * @param paymentRequest payment details accepted by the gateway
   * @return the bank response, or {@code null} when the bank call fails
   */
  public BankPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    try {
      return restTemplate.postForObject(
          bankPaymentsUrl,
          new BankPaymentRequest(paymentRequest),
          BankPaymentResponse.class);
    } catch (RestClientException ex) {
      LOG.warn("Bank payment authorization request failed. url={}", bankPaymentsUrl, ex);
      return null;
    }
  }
}
