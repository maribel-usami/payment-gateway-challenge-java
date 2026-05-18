package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankClientException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
   * @return the bank response
   * @throws BankClientException when the bank call fails or returns an invalid response
   */
  public BankPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    try {
      BankPaymentResponse response = restTemplate.postForObject(
          bankPaymentsUrl,
          new BankPaymentRequest(paymentRequest),
          BankPaymentResponse.class);

      if (response == null) {
        throw new BankClientException("BANK_API_ERROR", "Bank returned an empty response");
      }

      return response;
    } catch (RestClientException ex) {
      LOG.warn("Bank payment authorization request failed. url={}", bankPaymentsUrl, ex);
      throw new BankClientException("BANK_API_ERROR", "Bank payment authorization request failed", ex);
    }
  }
}
