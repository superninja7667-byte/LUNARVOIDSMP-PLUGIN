package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlaceholderValuesTest {
    private final UUID id=UUID.randomUUID();
    @Test void preservesExactCentsFormattingAndWholeStackSellAmounts() {
        var values=new PlaceholderValues();
        values.player(id,new PlaceholderValues.PlayerValues(".Bedrock_Player",123456L,71,64000000L));
        assertEquals(".Bedrock_Player",values.resolve(id,"name"));
        assertEquals("1234.56",values.resolve(id,"balance"));
        assertEquals("$1,234.56",values.resolve(id,"balance_formatted"));
        assertEquals("1.2K",values.resolve(id,"balance_short"));
        assertEquals("71",values.resolve(id,"ping"));
        assertEquals("640000.00",values.resolve(id,"held_sell_value"));
        assertEquals("$640,000.00",values.resolve(id,"held_sell_value_formatted"));
        assertEquals("true",values.resolve(id,"held_sellable"));
    }
    @Test void distinguishesUnavailableDataFromActualZeroAndUnsellableStacks() {
        var values=new PlaceholderValues();
        assertEquals("",values.resolve(id,"balance"));
        assertEquals("",values.resolve(null,"online"));
        values.player(id,new PlaceholderValues.PlayerValues("Player",null,-1,null));
        assertEquals("",values.resolve(id,"balance"));
        assertEquals("",values.resolve(id,"ping"));
        assertEquals("",values.resolve(id,"held_sell_value"));
        assertEquals("false",values.resolve(id,"held_sellable"));
        values.player(id,new PlaceholderValues.PlayerValues("Player",0L,0,null));
        assertEquals("0.00",values.resolve(id,"balance"));
    }
    @Test void globalValuesWorkWithoutAPlayerAndInvalidOrOfflineLookupsDoNotInventData() {
        var values=new PlaceholderValues(); values.server(12,20.4);
        assertEquals("12",values.resolve(null,"online")); assertEquals("20.00",values.resolve(null,"tps"));
        values.server(0,18.126); assertEquals("18.13",values.resolve(id,"tps"));
        values.server(0,Double.NaN); assertEquals("",values.resolve(id,"tps"));
        assertEquals(id.toString(),values.resolve(id,"uuid"));
        assertEquals("",values.resolve(id,"name")); assertNull(values.resolve(id,"unknown"));
    }
    @Test void finiteBalancesRoundToCentsAndInvalidValuesAreNeverDisplayedAsMoney() {
        assertEquals(1235L,PlaceholderValues.cents(12.345));
        assertEquals(-1235L,PlaceholderValues.cents(-12.345));
        assertEquals(0L,PlaceholderValues.cents(0));
        assertNull(PlaceholderValues.cents(Double.NaN)); assertNull(PlaceholderValues.cents(Double.POSITIVE_INFINITY));
        assertNull(PlaceholderValues.cents(1e50));
        var values=new PlaceholderValues(); values.player(id,new PlaceholderValues.PlayerValues("P",Long.MAX_VALUE,0,null));
        assertEquals("92233720368547758.07",values.resolve(id,"balance"));
        assertEquals("$92,233,720,368,547,758.07",values.resolve(id,"balance_formatted"));
    }
    @Test void disconnectAndProviderShutdownClearCachedValues() {
        var values=new PlaceholderValues(); values.player(id,new PlaceholderValues.PlayerValues("P",500L,0,null));
        assertTrue(values.contains(id)); values.forget(id); assertFalse(values.contains(id));
        assertEquals("",values.resolve(id,"balance"));
        values.server(5,20); values.clear(); assertEquals("",values.resolve(null,"online"));
    }
}
