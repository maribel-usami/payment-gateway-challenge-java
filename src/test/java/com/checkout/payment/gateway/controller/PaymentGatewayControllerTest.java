package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  private static final String VALID_AUTHORIZED_CARD = "2222405343248877";
  private static final String VALID_DECLINED_CARD = "2222405343248878";

  @Autowired
  private MockMvc mvc;
  @Autowired
  private PaymentsRepository paymentsRepository;
  @MockBean
  private BankClient bankClient;

  @Nested
  class RetrievePayment {

    @Test
    void whenPaymentWithIdExistsThenPaymentIsReturned() throws Exception {
      PostPaymentResponse payment = new PostPaymentResponse();
      payment.setId(UUID.randomUUID());
      payment.setAmount(10);
      payment.setCurrency("USD");
      payment.setStatus(PaymentStatus.AUTHORIZED);
      payment.setExpiryMonth(12);
      payment.setExpiryYear(2024);
      payment.setCardNumberLastFour("4321");

      paymentsRepository.add(payment);

      mvc.perform(get("/payment/" + payment.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
          .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
          .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
          .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
          .andExpect(jsonPath("$.amount").value(payment.getAmount()));
    }

    @Test
    void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
      mvc.perform(get("/payment/" + UUID.randomUUID()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Page not found"));
    }

    @Test
    void whenPaymentIdIsNotUuidThen400IsReturned() throws Exception {
      mvc.perform(get("/payment/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Invalid request parameter"));
    }
  }

  @Nested
  class RequestBodyErrors {

    @Test
    void whenPaymentRequestBodyIsMalformedThen400IsReturned() throws Exception {
      mvc.perform(post("/payment")
              .contentType(MediaType.APPLICATION_JSON)
              .content(new byte[] {'{'}))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Invalid request body"));
    }

    @Test
    void whenPaymentRequestBodyIsJsonNullThen400IsReturned() throws Exception {
      mvc.perform(post("/payment")
              .contentType(MediaType.APPLICATION_JSON)
              .content("null"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").value("Invalid request body"));
    }
  }

  @Nested
  class GatewayValidation {

    @Test
    void whenCardNumberIsTooShortThenPaymentIsRejected() throws Exception {
      performCreatePayment(paymentRequest("123", 12, 2030, "GBP", 100, "123"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value(""))
          .andExpect(jsonPath("$.expiryMonth").value(12))
          .andExpect(jsonPath("$.expiryYear").value(2030))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void whenCardNumberHasNonNumericCharactersThenPaymentIsRejected() throws Exception {
      performCreatePayment(paymentRequest("abcdefghijklmn", 12, 2030, "GBP", 100, "123"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value(""));
    }

    @Test
    void whenExpiryYearIsOutsideSupportedRangeThenPaymentIsRejected() throws Exception {
      performCreatePayment(paymentRequest(VALID_AUTHORIZED_CARD, 12, 1000000000, "GBP", 100, "123"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
          .andExpect(jsonPath("$.expiryYear").value(1000000000));
    }
  }

  @Nested
  class BankAuthorization {

    @Test
    void whenBankAuthorizesPaymentThenPaymentIsAuthorized() throws Exception {
      when(bankClient.processPayment(any(PostPaymentRequest.class)))
          .thenReturn(new BankPaymentResponse(true, UUID.randomUUID().toString()));

      performCreatePayment(validPaymentRequest(VALID_AUTHORIZED_CARD))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
          .andExpect(jsonPath("$.expiryMonth").value(12))
          .andExpect(jsonPath("$.expiryYear").value(2030))
          .andExpect(jsonPath("$.currency").value("GBP"))
          .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void whenBankDeclinesPaymentThenPaymentIsDeclined() throws Exception {
      when(bankClient.processPayment(any(PostPaymentRequest.class)))
          .thenReturn(new BankPaymentResponse(false, ""));

      performCreatePayment(validPaymentRequest(VALID_DECLINED_CARD))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8878"));
    }

    @Test
    void whenBankClientFailsThenPaymentIsRejected() throws Exception {
      when(bankClient.processPayment(any(PostPaymentRequest.class))).thenReturn(null);

      performCreatePayment(validPaymentRequest(VALID_AUTHORIZED_CARD))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8877"));
    }
  }

  @Nested
  class ResponseDetails {

    @Test
    void whenCardNumberLastFourHasLeadingZerosThenTheyArePreserved() throws Exception {
      when(bankClient.processPayment(any(PostPaymentRequest.class)))
          .thenReturn(new BankPaymentResponse(true, UUID.randomUUID().toString()));

      performCreatePayment(validPaymentRequest("2222405343240042"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("0042"));
    }
  }

  @Nested
  class Persistence {

    @Test
    void whenPaymentIsCreatedThenItCanBeRetrievedById() throws Exception {
      when(bankClient.processPayment(any(PostPaymentRequest.class)))
          .thenReturn(new BankPaymentResponse(true, UUID.randomUUID().toString()));

      MvcResult postResult = performCreatePayment(validPaymentRequest(VALID_AUTHORIZED_CARD))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").exists())
          .andReturn();

      String paymentId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.id");

      mvc.perform(get("/payment/" + paymentId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(paymentId))
          .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value("8877"));
    }
  }

  private ResultActions performCreatePayment(String requestBody) throws Exception {
    return mvc.perform(post("/payment")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody));
  }

  private String validPaymentRequest(String cardNumber) {
    return paymentRequest(cardNumber, 12, 2030, "GBP", 100, "123");
  }

  private String paymentRequest(String cardNumber, int expiryMonth, int expiryYear,
                                String currency, long amount, String cvv) {
    return """
        {
          "card_number": "%s",
          "expiry_month": %d,
          "expiry_year": %d,
          "currency": "%s",
          "amount": %d,
          "cvv": "%s"
        }
        """.formatted(cardNumber, expiryMonth, expiryYear, currency, amount, cvv);
  }
}
