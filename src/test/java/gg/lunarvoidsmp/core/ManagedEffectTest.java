package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManagedEffectTest {
    static final class Effect implements ManagedEffect.Effect {
        String value; boolean owned,allow=true,allowRemove=true; int grants,removals;
        public boolean present() { return value!=null; }
        public boolean matches() { return "lunar".equals(value); }
        public boolean owned() { return owned; }
        public void owned(boolean v) { owned=v; }
        public boolean grant() { grants++; if(allow) value="lunar"; return allow; }
        public void remove() { removals++; if(allowRemove) value=null; }
    }
    @Test void offDoesNotGrantAndOnGrantsOnlyOnceThenOffRemovesItsEffect() {
        var e=new Effect(); ManagedEffect.apply(false,e); assertEquals(0,e.grants);
        ManagedEffect.apply(true,e); ManagedEffect.apply(true,e);
        assertTrue(e.owned); assertEquals(1,e.grants);
        ManagedEffect.apply(false,e); assertNull(e.value); assertFalse(e.owned); assertEquals(1,e.removals);
    }
    @Test void existingPotionIsNeitherOverwrittenNorRemovedWhenSettingChanges() {
        var e=new Effect(); e.value="potion";
        ManagedEffect.apply(true,e); ManagedEffect.apply(false,e);
        assertEquals("potion",e.value); assertEquals(0,e.grants); assertEquals(0,e.removals);
        e.value="lunar"; // Even an identical effect without our marker belongs to another source.
        ManagedEffect.apply(false,e); assertEquals("lunar",e.value);
    }
    @Test void lostEffectIsReappliedWhileOnButNotWhileOff() {
        var e=new Effect(); ManagedEffect.apply(true,e);
        e.value=null; ManagedEffect.apply(true,e);
        assertEquals(2,e.grants); assertTrue(e.owned);
        ManagedEffect.apply(false,e); e.value=null; ManagedEffect.apply(false,e);
        assertEquals(2,e.grants);
    }
    @Test void externalReplacementIsPreservedAndPersistedOwnedEffectCanBeCleanedUp() {
        var e=new Effect(); ManagedEffect.apply(true,e); e.value="external";
        ManagedEffect.apply(false,e); assertEquals("external",e.value); assertFalse(e.owned);
        e.value="lunar"; e.owned=true; // Restored marker/effect after an interrupted session.
        ManagedEffect.apply(false,e); assertNull(e.value); assertFalse(e.owned);
    }
    @Test void cancelledGrantDoesNotClaimOwnershipAndCleanupIsIdempotent() {
        var e=new Effect(); e.allow=false; ManagedEffect.apply(true,e);
        assertFalse(e.owned); assertNull(e.value);
        e.allow=true; ManagedEffect.apply(true,e); ManagedEffect.release(e); ManagedEffect.release(e);
        assertEquals(1,e.removals); assertFalse(e.owned);
    }
    @Test void cancelledRemovalKeepsOwnershipUntilAnAllowedRetry() {
        var e=new Effect(); ManagedEffect.apply(true,e); e.allowRemove=false;
        ManagedEffect.apply(false,e); assertTrue(e.owned); assertEquals("lunar",e.value);
        e.allowRemove=true; ManagedEffect.apply(false,e);
        assertFalse(e.owned); assertNull(e.value); assertEquals(2,e.removals);
    }
}
