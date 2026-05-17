package com.checkout.payment.gateway.util;

/**
 * Utility methods for card data handling.
 */
public final class CardUtils {

  private CardUtils() {
  }

  /**
   * Returns the last four characters of a card number, or an empty string when unavailable.
   */
  public static String getCardNumberLastFour(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "";
    }
    return cardNumber.substring(cardNumber.length() - 4);
  }

}
