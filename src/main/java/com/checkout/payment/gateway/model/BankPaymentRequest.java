package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request of payment to bank
 */
public class BankPaymentRequest {

  @JsonProperty("card_number")
  private String cardNumber;
  @JsonProperty("expiry_date")
  private String expiryDate;
  private String currency;
  private int amount;
  private String cvv;

  /**
   * Constructor, with payment request object from merchant
   * @param paymentRequest
   */
  public BankPaymentRequest(PostPaymentRequest paymentRequest) {
    this.cardNumber = paymentRequest.getCardNumber();
    this.expiryDate = String.format("%02d/%d",
        paymentRequest.getExpiryMonth(),
        paymentRequest.getExpiryYear());
    this.currency = paymentRequest.getCurrency();
    this.amount = paymentRequest.getAmount();
    this.cvv = paymentRequest.getCvv();
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }
}
