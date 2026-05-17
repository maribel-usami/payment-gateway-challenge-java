package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @MockBean
  BankClient bankClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
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
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  void whenPaymentIdIsNotUuidThen400IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid request parameter"));
  }

  @Test
  void whenPaymentRequestBodyIsMalformedThen400IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new byte[] {'{'}))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid request body"));
  }

  @Test
  void whenPaymentRequestIsInvalidThenPaymentIsRejected() throws Exception {
    String request = """
        {
          "card_number": "123",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(0))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenBankAuthorizesPaymentThenPaymentIsAuthorized() throws Exception {
    when(bankClient.processPayment(any(PostPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, UUID.randomUUID().toString()));

    String request = """
        {
          "card_number": "2222405343248877",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenPaymentIsCreatedThenItCanBeRetrievedById() throws Exception {
    when(bankClient.processPayment(any(PostPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, UUID.randomUUID().toString()));

    String request = """
        {
          "card_number": "2222405343248877",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    MvcResult postResult = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    String paymentId = JsonPath.read(postResult.getResponse().getContentAsString(), "$.id");

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877));
  }

  @Test
  void whenBankDeclinesPaymentThenPaymentIsDeclined() throws Exception {
    when(bankClient.processPayment(any(PostPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, ""));

    String request = """
        {
          "card_number": "2222405343248878",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8878));
  }
}
