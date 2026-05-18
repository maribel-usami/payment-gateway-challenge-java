package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.Currency;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankClientException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.util.CardUtils;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles payment lookup, validation, bank authorization, and result persistence.
 */
@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  /**
   * Retrieves a payment previously created by the gateway.
   *
   * @param id gateway-generated payment ID
   * @return stored payment details
   */
  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  /**
   * Processes a payment request and stores the resulting gateway payment record.
   *
   * @param paymentRequest merchant payment request
   * @return gateway payment response with the final status
   */
  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    PostPaymentResponse response = createPaymentResponse(paymentRequest);

    if (!isValid(paymentRequest)) {
      response.setStatus(PaymentStatus.REJECTED);
      paymentsRepository.save(response);
      return response;
    }

    try {
      BankPaymentResponse bankResponse = bankClient.processPayment(paymentRequest);
      response.setStatus(bankResponse.isAuthorized()
          ? PaymentStatus.AUTHORIZED
          : PaymentStatus.DECLINED);
    } catch (BankClientException ex) {
      LOG.warn("Bank authorization failed. code={}", ex.getCode(), ex);
      response.setStatus(PaymentStatus.REJECTED);
    }
    paymentsRepository.save(response);
    return response;
  }

  /**
   * Creates the gateway payment response that will be saved for later lookup.
   */
  private PostPaymentResponse createPaymentResponse(PostPaymentRequest paymentRequest) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    if (paymentRequest == null) {
      return response;
    }
    response.setCardNumberLastFour(isValidCardNumber(paymentRequest.getCardNumber())
        ? CardUtils.getCardNumberLastFour(paymentRequest.getCardNumber())
        : "");
    response.setExpiryMonth(paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest.getCurrency());
    response.setAmount(paymentRequest.getAmount());
    return response;
  }

  /**
   * Validates gateway-level payment request rules before calling the bank.
   */
  private boolean isValid(PostPaymentRequest paymentRequest) {
    if (paymentRequest == null) {
      return false;
    }
    return isValidCardNumber(paymentRequest.getCardNumber())
        && isValidCvv(paymentRequest.getCvv())
        && isValidExpiryDate(paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear())
        && isValidCurrency(paymentRequest.getCurrency())
        && paymentRequest.getAmount() > 0;
  }

  /**
   * Card numbers must contain 14 to 19 digits.
   */
  private boolean isValidCardNumber(String cardNumber) {
    return cardNumber != null && cardNumber.matches("\\d{14,19}");
  }

  /**
   * CVV must contain 3 or 4 digits.
   */
  private boolean isValidCvv(String cvv) {
    return cvv != null && cvv.matches("\\d{3,4}");
  }

  /**
   * Expiry date must be a valid month/year in the future.
   */
  private boolean isValidExpiryDate(int expiryMonth, int expiryYear) {
    if (expiryMonth < 1 || expiryMonth > 12) {
      return false;
    }
    try {
      return YearMonth.of(expiryYear, expiryMonth).isAfter(YearMonth.now());
    } catch (DateTimeException ex) {
      return false;
    }
  }

  /**
   * Currency must be one of the gateway-supported ISO currency codes.
   */
  private boolean isValidCurrency(String currency) {
    return Currency.isSupported(currency);
  }
}
