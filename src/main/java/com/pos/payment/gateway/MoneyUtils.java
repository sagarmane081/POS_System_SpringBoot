package com.pos.payment.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Both Stripe and Razorpay expect amounts as an integer count of the
 * currency's smallest unit (e.g. cents, paise), not a decimal major-unit value.
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static long toSmallestUnit(BigDecimal amount) {

        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
