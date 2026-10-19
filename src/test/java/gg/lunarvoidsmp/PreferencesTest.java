package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Store;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PreferencesTest {
    @TempDir Path folder;
    @Test void itemPriceStartsOffAndExplicitChoicePersistsAcrossRestart() throws Exception {
        UUID player=UUID.randomUUID();
        try(var store=new Store(folder.resolve("lunar.db"))) {
            var defaults=new MemoryConfiguration(); defaults.set("item_price",true);
            var preferences=new Preferences(store,defaults);
            preferences.load(player);
            assertFalse(preferences.enabled(player,"item_price"));
            preferences.set(player,"item_price",true);
        }
        try(var store=new Store(folder.resolve("lunar.db"))) {
            var preferences=new Preferences(store,null); preferences.load(player);
            assertTrue(preferences.enabled(player,"item_price"));
            preferences.load(UUID.randomUUID());
            assertFalse(preferences.enabled(UUID.randomUUID(),"item_price"));
        }
    }
    @Test void nightVisionStartsOffAndBothChoicesSurviveRestart() throws Exception {
        UUID on=UUID.randomUUID(),off=UUID.randomUUID();
        try(var store=new Store(folder.resolve("night.db"))) {
            var defaults=new MemoryConfiguration(); defaults.set("night_vision",true);
            var preferences=new Preferences(store,defaults);
            preferences.load(on); preferences.load(off);
            assertEquals(Boolean.FALSE,preferences.get(on,"night_vision"));
            assertFalse(preferences.enabled(off,"night_vision"));
            preferences.set(on,"night_vision",true); preferences.set(off,"night_vision",false);
        }
        try(var store=new Store(folder.resolve("night.db"))) {
            var preferences=new Preferences(store,null); preferences.load(on); preferences.load(off);
            assertTrue(preferences.enabled(on,"night_vision"));
            assertFalse(preferences.enabled(off,"night_vision"));
        }
    }
}
