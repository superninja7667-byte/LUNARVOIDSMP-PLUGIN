package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Money;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.function.Function;

/** Returns a display copy only. Never write this item to an inventory or escrow. */
final class ItemPriceTooltip {
    private final Function<ItemStack,Long> sellValue;
    ItemPriceTooltip(Function<ItemStack,Long> sellValue) { this.sellValue=sellValue; }
    ItemStack project(ItemStack original) {
        if(original==null || original.isEmpty()) return original;
        Long value=sellValue.apply(original);
        ItemStack display=original.clone();
        var meta=display.getItemMeta();
        var lore=meta.lore()==null?new ArrayList<Component>():new ArrayList<>(meta.lore());
        Component line=value==null?Component.text("Not sellable in /sell",NamedTextColor.GRAY)
            :Component.text("Sell: ",NamedTextColor.GRAY)
                .append(Component.text("$",NamedTextColor.GREEN))
                .append(Component.text(Money.full(value).substring(1),NamedTextColor.WHITE));
        lore.add(line.decoration(TextDecoration.ITALIC,false));
        meta.lore(lore); display.setItemMeta(meta);
        return display;
    }
}
