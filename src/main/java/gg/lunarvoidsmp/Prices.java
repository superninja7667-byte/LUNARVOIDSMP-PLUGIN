package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Money;
import gg.lunarvoidsmp.core.SellValue;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import java.util.*;

final class Prices {
    record Price(Long buy,Long sell) {}
    private final Map<Material,Price> prices=new EnumMap<>(Material.class);
    Prices(ConfigurationSection config) {
        if(config==null) return;
        for(String key:config.getKeys(false)) {
            Material material=Material.matchMaterial(key);
            if(material==null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid priced material: "+key);
            Long sell=value(config,key+".sell");
            if(sell!=null) SellValue.validateUnit(sell);
            prices.put(material,new Price(value(config,key+".buy"),sell));
        }
    }
    private Long value(ConfigurationSection config,String path) { Object raw=config.get(path); return raw==null?null:Money.parse(raw.toString()); }
    Collection<Material> materials() { return Collections.unmodifiableSet(prices.keySet()); }
    Long unitSell(Material material) { Price p=prices.get(material); return p==null?null:p.sell(); }
    Long buy(Material material) { Price p=prices.get(material); return p==null?null:p.buy(); }
    Long sell(ItemStack item) {
        if(item==null || item.getType().isAir() || item.getAmount()<=0 || item.hasItemMeta()) return null;
        Price p=prices.get(item.getType()); return p==null || p.sell()==null ? null : SellValue.stack(p.sell(),item.getAmount());
    }
}
