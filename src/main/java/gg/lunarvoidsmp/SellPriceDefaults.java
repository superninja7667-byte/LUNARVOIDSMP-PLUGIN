package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Money;
import gg.lunarvoidsmp.core.SellValue;
import org.bukkit.configuration.ConfigurationSection;

/** Imports approved defaults and repairs the missing Vault price on upgrades. */
final class SellPriceDefaults {
    private SellPriceDefaults() {}
    static boolean install(ConfigurationSection config,ConfigurationSection defaults) {
        var existing=config.getConfigurationSection("prices");
        boolean empty=existing==null || existing.getKeys(false).isEmpty();
        boolean changed=false;
        for(String material:defaults.getKeys(false)) {
            // Earlier builds populated everything except VAULT under the old cap.
            // Explicit existing material sections, including disabled prices, are retained.
            if(!empty && (!material.equals("VAULT") || existing.contains(material))) continue;
            String price=defaults.getString(material+".sell");
            SellValue.validateUnit(Money.parse(price));
            config.set("prices."+material+".sell",price);
            changed=true;
        }
        return changed;
    }
}
