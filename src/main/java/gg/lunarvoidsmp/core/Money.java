package gg.lunarvoidsmp.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Amounts are integer cents in storage. Vault conversion happens only at its boundary. */
public final class Money {
    private Money() {}
    public static long parse(String input) {
        if (input == null || !input.matches("[0-9]{1,12}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Enter a positive amount with at most two decimal places.");
        long cents = new BigDecimal(input).movePointRight(2).longValueExact();
        if (cents <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        return cents;
    }
    public static double vault(long cents) { return BigDecimal.valueOf(cents, 2).doubleValue(); }
    public static String full(long cents) { return String.format(Locale.US, "$%,.2f", vault(cents)); }
    public static String shortAmount(long cents) {
        return NumberFormatter.shorten(BigDecimal.valueOf(cents,2));
    }
}
