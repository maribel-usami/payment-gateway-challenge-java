package com.checkout.payment.gateway.util;

/**
 * Util class dealing with card info
 */
public final class CardUtils {

  private CardUtils() {
  }

  /**
   * Get last four digits of card number
   * @param cardNumber
   * @return lastFourDigit
   */
  public static String getCardNumberLastFour(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "";
    }
    return cardNumber.substring(cardNumber.length() - 4);
  }

  public static int getCardNumberLastFourAsInt(String cardNumber) {
    String lastFour = getCardNumberLastFour(cardNumber);
    if (lastFour.isEmpty()) {
      return 0;
    }
    return Integer.parseInt(lastFour);
  }
}
