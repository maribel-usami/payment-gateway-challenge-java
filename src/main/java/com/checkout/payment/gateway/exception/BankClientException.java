package com.checkout.payment.gateway.exception;

public class BankClientException extends RuntimeException {

  private final String code;

  public BankClientException(String code, String message) {
    super(message);
    this.code = code;
  }

  public BankClientException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
