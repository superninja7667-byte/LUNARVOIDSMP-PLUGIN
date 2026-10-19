package gg.lunarvoidsmp;

import gg.lunarvoidsmp.core.Store;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.sql.SQLException;

final class Items {
    private final Store store;
    Items(Store store) { this.store=store; }
    String take(Player player,int slot,ItemStack expected,String purpose) throws SQLException {
        ItemStack actual=player.getInventory().getItem(slot);
        if(actual==null || !actual.equals(expected)) throw new IllegalArgumentException("The item changed. Please try again.");
        // If shutdown interrupts the inventory/database boundary, REVIEW retains the exact bytes.
        String id=store.addItem(player.getUniqueId().toString(),expected.serializeAsBytes(),"REMOVING",purpose);
        player.getInventory().setItem(slot,null); player.saveData();
        if(!store.itemState(id,"REMOVING","HELD")) throw new IllegalStateException("Item escrow could not be finalized.");
        return id;
    }
    boolean give(Player player,Store.Item held) throws SQLException {
        if(!held.owner().equals(player.getUniqueId().toString()) || !held.state().equals("HELD") || !held.purpose().equals("CLAIM")) return false;
        ItemStack stack=ItemStack.deserializeBytes(held.payload());
        if(!fits(player,stack) || !store.itemState(held.id(),"HELD","DELIVERING")) return false;
        // No world drops. Unexpected partial delivery stays quarantined for operator inspection.
        if(!player.getInventory().addItem(stack).isEmpty()) {
            store.itemState(held.id(),"DELIVERING","REVIEW"); throw new IllegalStateException("Delivery needs review; item record "+held.id());
        }
        player.saveData();
        if(!store.itemState(held.id(),"DELIVERING","DELIVERED")) throw new IllegalStateException("Delivery needs review: "+held.id());
        return true;
    }
    boolean fits(Player player,ItemStack stack) {
        int needed=stack.getAmount();
        for(ItemStack item:player.getInventory().getStorageContents()) {
            if(item==null || item.getType().isAir()) needed-=Math.min(stack.getMaxStackSize(),player.getInventory().getMaxStackSize());
            else if(item.isSimilar(stack)) needed-=Math.max(0,Math.min(item.getMaxStackSize(),player.getInventory().getMaxStackSize())-item.getAmount());
            if(needed<=0) return true;
        }
        return false;
    }
    void claim(Player player) throws SQLException {
        for(Store.Item item:store.items(player.getUniqueId().toString())) if(item.purpose().equals("CLAIM")) give(player,item);
    }
    void returnSell(Player player) throws SQLException {
        store.update("UPDATE items SET purpose='CLAIM' WHERE owner=? AND state='HELD' AND purpose='SELL'",player.getUniqueId().toString());
        claim(player);
    }
}
