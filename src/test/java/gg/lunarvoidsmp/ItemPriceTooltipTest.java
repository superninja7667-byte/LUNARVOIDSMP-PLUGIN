package gg.lunarvoidsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemPriceTooltipTest {
    // Fake only Bukkit's server-owned ItemStack/meta boundary; projection uses real Adventure components.
    static final class Stack extends ItemStack {
        final Material material; final int count;
        List<Component> lore;
        Stack(Material material,int count,List<Component> lore) { this.material=material; this.count=count; this.lore=List.copyOf(lore); }
        @Override public Material getType() { return material; }
        @Override public boolean isEmpty() { return material==Material.AIR || count<=0; }
        @Override public int getAmount() { return count; }
        @Override public Stack clone() { return new Stack(material,count,lore); }
        @Override public ItemMeta getItemMeta() {
            var state=new ArrayList<>(lore);
            return (ItemMeta)Proxy.newProxyInstance(ItemMeta.class.getClassLoader(),new Class<?>[]{ItemMeta.class},(proxy,method,args)-> {
                if(method.getName().equals("lore")) {
                    if(args==null || args.length==0) return List.copyOf(state);
                    state.clear(); state.addAll((List<Component>)args[0]); return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }
        @Override public boolean setItemMeta(ItemMeta meta) { lore=List.copyOf(meta.lore()); return true; }
    }
    @Test void showsWholeStackPayoutOnACopyAndPreservesOriginalLoreAndQuantity() {
        var original=new Stack(Material.DIAMOND,64,List.of(Component.text("Original",NamedTextColor.GOLD)));
        var projection=new ItemPriceTooltip(item->{ assertSame(original,item); return 64_000_000L; });
        Stack display=(Stack)projection.project(original);
        assertNotSame(original,display);
        assertEquals(1,original.lore.size()); assertEquals(64,original.count);
        assertEquals(original.lore.getFirst(),display.lore.getFirst());
        assertEquals("Sell: $640,000.00",PlainTextComponentSerializer.plainText().serialize(display.lore.getLast()));
        assertEquals(NamedTextColor.GREEN,display.lore.getLast().children().getFirst().color());
        assertEquals(NamedTextColor.WHITE,display.lore.getLast().children().get(1).color());
        assertEquals(2,((Stack)projection.project(original)).lore.size());
    }
    @Test void unsellableItemsAreNotGivenAnInventedPayoutAndAirIsUntouched() {
        var projection=new ItemPriceTooltip(item->null);
        Stack original=new Stack(Material.SHULKER_BOX,1,List.of());
        Stack display=(Stack)projection.project(original);
        assertEquals("Not sellable in /sell",PlainTextComponentSerializer.plainText().serialize(display.lore.getLast()));
        assertTrue(original.lore.isEmpty()); assertNull(projection.project(null));
        Stack air=new Stack(Material.AIR,0,List.of()); assertSame(air,projection.project(air));
    }
}
