package com.checkout.payment.gateway.enums;

import java.util.Arrays;

/**
 * Currencies currently supported by the gateway.
 */
public enum Currency {
  GBP,
  USD,
  EUR;

  /**
   * Checks whether the supplied currency code is supported.
   */
  public static boolean isSupported(String currency) {
    return Arrays.stream(values())
        .anyMatch(supportedCurrency -> supportedCurrency.name().equals(currency));
  }
}
