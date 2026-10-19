package gg.lunarvoidsmp;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SellPriceDefaultsTest {
    private YamlConfiguration defaults() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getResourceAsStream("/sell-prices.yml"),StandardCharsets.UTF_8));
    }
    @Test void freshInstallationImportsEveryApprovedPriceIncludingVault() {
        var config=new MemoryConfiguration();
        assertTrue(SellPriceDefaults.install(config,defaults().getConfigurationSection("prices")));
        assertEquals(1238,config.getConfigurationSection("prices").getKeys(false).size());
        assertEquals("30000",config.getString("prices.VAULT.sell"));
        assertFalse(SellPriceDefaults.install(config,defaults().getConfigurationSection("prices")));
    }
    @Test void upgradingPopulatedPricesAddsVaultWithoutChangingOtherPrices() {
        var config=new MemoryConfiguration(); config.set("prices.DIAMOND.sell","123.45");
        assertTrue(SellPriceDefaults.install(config,defaults().getConfigurationSection("prices")));
        assertEquals("30000",config.getString("prices.VAULT.sell"));
        assertEquals("123.45",config.getString("prices.DIAMOND.sell"));
        assertEquals(2,config.getConfigurationSection("prices").getKeys(false).size());
    }
    @Test void explicitlyCustomizedOrDisabledVaultPricesArePreserved() {
        var config=new MemoryConfiguration(); config.set("prices.VAULT.sell","15000");
        assertFalse(SellPriceDefaults.install(config,defaults().getConfigurationSection("prices")));
        assertEquals("15000",config.getString("prices.VAULT.sell"));
        config.set("prices.VAULT.sell",null); config.createSection("prices.VAULT");
        assertFalse(SellPriceDefaults.install(config,defaults().getConfigurationSection("prices")));
        assertNull(config.get("prices.VAULT.sell"));
    }
}
