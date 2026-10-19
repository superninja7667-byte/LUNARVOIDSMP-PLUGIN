package gg.lunarvoidsmp.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberFormatter {
    private static final String[] UNITS={"","K","M","B","T","Q","P","E","Z","Y"};
    private NumberFormatter() {}
    public static String shorten(BigDecimal amount) {
        int unit=0;
        while(amount.abs().compareTo(BigDecimal.valueOf(1000))>=0 && unit<UNITS.length-1) { amount=amount.movePointLeft(3); unit++; }
        if(unit==0) return amount.setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        amount=amount.setScale(1,RoundingMode.HALF_UP);
        if(amount.abs().compareTo(BigDecimal.valueOf(1000))>=0 && unit<UNITS.length-1) { amount=amount.movePointLeft(3).setScale(1,RoundingMode.HALF_UP); unit++; }
        return amount.toPlainString()+UNITS[unit];
    }
}
