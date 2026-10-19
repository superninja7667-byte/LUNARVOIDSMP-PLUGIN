package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.PlaceholderValues;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class LunarExpansionTest {
    private final UUID id=UUID.randomUUID();
    private final OfflinePlayer player=(OfflinePlayer)Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(),new Class<?>[]{OfflinePlayer.class},(proxy,method,args)-> {
        if(method.getName().equals("getUniqueId")) return id;
        throw new UnsupportedOperationException(method.getName());
    });
    private PlaceholderValues values() {
        var values=new PlaceholderValues(); values.server(4,20);
        values.player(id,new PlaceholderValues.PlayerValues("Player",50000L,50,null)); return values;
    }
    @Test void internalExpansionAdvertisesItsKeysAndPersistsThroughPapiReload() {
        var expansion=new LunarExpansion("test-version",values(),(id,key)->null,key->"",(p,text)->text);
        assertEquals("lunar",expansion.getIdentifier()); assertEquals("test-version",expansion.getVersion());
        assertTrue(expansion.persist());
        assertTrue(expansion.getPlaceholders().contains("%lunar_held_sell_value%"));
        assertTrue(expansion.getPlaceholders().contains("%lunar_setting_night_vision%"));
        assertTrue(expansion.getPlaceholders().contains("%lunar_line_money%"));
    }
    @Test void resolvesRawAndGlobalValuesAndLeavesUnknownPlaceholdersToPapi() {
        var expansion=new LunarExpansion("test",values(),(id,key)->null,key->"",(p,text)->text);
        assertEquals("500.00",expansion.onRequest(player,"BALANCE"));
        assertEquals("4",expansion.onRequest(null,"online"));
        assertEquals("",expansion.onRequest(null,"balance"));
        assertNull(expansion.onRequest(player,"does_not_exist")); assertNull(expansion.onRequest(player,null));
    }
    @Test void settingsKeepTrueFalseUnsetAndDoNotGuessOfflinePreferences() {
        Map<String,Boolean> settings=Map.of("night_vision",true,"item_price",false);
        var values=values();
        var expansion=new LunarExpansion("test",values,(id,key)->settings.get(key),key->"",(p,text)->text);
        assertEquals("true",expansion.onRequest(player,"setting_night_vision"));
        assertEquals("false",expansion.onRequest(player,"setting_item_price"));
        assertEquals("unset",expansion.onRequest(player,"setting_money"));
        assertNull(expansion.onRequest(player,"setting_unknown"));
        values.forget(id); assertEquals("",expansion.onRequest(player,"setting_night_vision"));
    }
    @Test void preservesFullOriginalTabLineAndDelegatesItsNestedPlaceholders() {
        String original="&aMoney: %vault_eco_balance% / %lunar_balance%";
        var expansion=new LunarExpansion("test",values(),(id,key)->null,key->original,(p,text)-> {
            assertSame(player,p); assertEquals(original,text);
            return text.replace("%vault_eco_balance%","500").replace("%lunar_balance%","500.00");
        });
        assertEquals("&aMoney: 500 / 500.00",expansion.onRequest(player,"line_money"));
    }
    @Test void disabledLinesAreEmptyAndCircularLineTemplatesTerminate() {
        var disabled=new LunarExpansion("test",values(),(id,key)->false,key->"original",(p,text)->{ fail("Disabled line must not invoke another expansion"); return text; });
        assertEquals("",disabled.onRequest(player,"line_money"));
        var reference=new AtomicReference<LunarExpansion>();
        var circular=new LunarExpansion("test",values(),(id,key)->true,key->"circular",(p,text)->reference.get().onRequest(p,"line_ping"));
        reference.set(circular); assertEquals("",circular.onRequest(player,"line_money"));
    }
    @Test void nestedExpansionFailureDoesNotPoisonLaterRenderingOnThatThread() {
        var calls=new AtomicInteger();
        var expansion=new LunarExpansion("test",values(),(id,key)->true,key->"original",(p,text)-> {
            if(calls.getAndIncrement()==0) throw new IllegalStateException("External expansion failed");
            return text;
        });
        assertThrows(IllegalStateException.class,()->expansion.onRequest(player,"line_money"));
        assertEquals("original",expansion.onRequest(player,"line_money"));
    }
}
