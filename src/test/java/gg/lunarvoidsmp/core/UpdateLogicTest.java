package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UpdateLogicTest {
    @Test void formatterSupportsAllRequestedUnitsAndNegativeBalances() {
        String[] units={"K","M","B","T","Q","P","E","Z","Y"};
        for(int i=0;i<units.length;i++) assertEquals("1.2"+units[i],NumberFormatter.shorten(new BigDecimal("1200").scaleByPowerOfTen(i*3)));
        assertEquals("-1.2M",NumberFormatter.shorten(new BigDecimal("-1200000")));
        assertEquals("1.0M",NumberFormatter.shorten(new BigDecimal("999999")));
        assertEquals("0",NumberFormatter.shorten(BigDecimal.ZERO));
        assertEquals("1.25",NumberFormatter.shorten(new BigDecimal("1.25")));
    }
    @Test void requestsExpireAndNewRequestsReplacePreviousTarget() {
        var state=new TpaRequests(); UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
        state.create(a,b,false,0); assertEquals(1,state.incoming(b,59_999).size());
        assertTrue(state.incoming(b,60_000).isEmpty());
        state.create(a,b,false,60_001); state.create(a,c,true,90_001);
        assertTrue(state.incoming(b,90_002).isEmpty()); assertTrue(state.incoming(c,90_002).getFirst().here());
    }
    @Test void selfRequestsAndCooldownCannotBypassSafety() {
        var state=new TpaRequests(); UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->state.create(a,a,false,0));
        state.create(a,b,false,0); assertThrows(IllegalArgumentException.class,()->state.create(a,b,true,29_999));
        assertFalse(state.create(a,b,false,30_000).here());
    }
    @Test void disconnectRemovesIncomingAndOutgoingRequests() {
        var state=new TpaRequests(); UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
        state.create(a,b,false,0); state.create(c,a,true,0); state.forget(a);
        assertTrue(state.incoming(b,1).isEmpty()); assertTrue(state.incoming(a,1).isEmpty());
    }
}
