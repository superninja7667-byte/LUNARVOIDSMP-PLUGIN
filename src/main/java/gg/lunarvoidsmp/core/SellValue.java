package gg.lunarvoidsmp.core;

/** Fixed server payouts; auction asking prices do not pass through this policy. */
public final class SellValue {
    public static final long MAX_UNIT_CENTS=3_000_000;
    private SellValue() {}
    public static long validateUnit(long cents) {
        if(cents<=0 || cents>MAX_UNIT_CENTS)
            throw new IllegalArgumentException("A /sell unit price must be greater than zero and at most $30,000.00.");
        return cents;
    }
    public static long stack(long unitCents,int amount) {
        validateUnit(unitCents);
        if(amount<=0) throw new IllegalArgumentException("A sell stack must contain at least one item.");
        return Math.multiplyExact(unitCents,amount);
    }
}
