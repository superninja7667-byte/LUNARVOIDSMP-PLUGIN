package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Money;
import gg.lunarvoidsmp.core.SellValue;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PriceTableTest {
    @Test void importedTableHasValidMaterialNamesAndPreservesVariantExclusions() {
        var stream=getClass().getResourceAsStream("/sell-prices.yml"); assertNotNull(stream);
        var table=YamlConfiguration.loadConfiguration(new InputStreamReader(stream,StandardCharsets.UTF_8)).getConfigurationSection("prices");
        assertNotNull(table); assertEquals(1238,table.getKeys(false).size());
        int aboveCap=0;
        for(String name:table.getKeys(false)) {
            assertDoesNotThrow(()->Material.valueOf(name),name);
            long cents=Money.parse(table.getString(name+".sell")); if(cents>SellValue.MAX_UNIT_CENTS) aboveCap++;
        }
        assertEquals(0,aboveCap); assertEquals(3_000_000,Money.parse(table.getString("VAULT.sell")));
        assertEquals(125,Money.parse(table.getString("INK_SAC.sell")));
        for(String excluded:Set.of("ENCHANTED_BOOK","POTION","SPLASH_POTION","LINGERING_POTION","TIPPED_ARROW")) assertFalse(table.contains(excluded));
    }
}
