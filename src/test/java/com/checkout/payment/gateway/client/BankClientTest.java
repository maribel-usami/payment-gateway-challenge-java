package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankClientException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class BankClientTest {

  private static final String BANK_PAYMENTS_URL = "http://bank.example/payments";

  @Test
  void whenBankServiceIsUnavailableThenBankClientExceptionIsThrown() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    BankClient bankClient = new BankClient(restTemplate, BANK_PAYMENTS_URL);

    when(restTemplate.postForObject(
        eq(BANK_PAYMENTS_URL),
        any(BankPaymentRequest.class),
        eq(BankPaymentResponse.class)))
        .thenThrow(new ResourceAccessException("Connection refused"));

    BankClientException exception = assertThrows(
        BankClientException.class,
        () -> bankClient.processPayment(validPaymentRequest()));

    assertEquals("BANK_API_ERROR", exception.getCode());
  }

  private PostPaymentRequest validPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }
}
