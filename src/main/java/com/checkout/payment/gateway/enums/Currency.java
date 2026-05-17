package com.checkout.payment.gateway.enums;

import java.util.Arrays;

/**
 * Currency types surpported for now
 */
public enum Currency {
  GBP,
  USD,
  EUR;

  /**
   * Check the input currency whether supported or not
   * @param currency
   * @return boolean
   */
  public static boolean isSupported(String currency) {
    return Arrays.stream(values())
        .anyMatch(supportedCurrency -> supportedCurrency.name().equals(currency));
  }
}
