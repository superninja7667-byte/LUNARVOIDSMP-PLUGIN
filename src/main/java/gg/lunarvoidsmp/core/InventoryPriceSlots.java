package gg.lunarvoidsmp.core;

/** Java inventory packet coordinates, independent of Bukkit menu slot conversion. */
public final class InventoryPriceSlots {
    private InventoryPriceSlots() {}
    public static boolean content(int container,int total,int slot) {
        // Window 0: crafting result/inputs 0..4, equipment/storage/offhand 5..45.
        if(container==0) return total==46 && slot>=5 && slot<46;
        // Other vanilla menus append the player's 27 storage + 9 hotbar slots.
        return container>0 && total>=36 && slot>=total-36 && slot<total;
    }
    public static boolean direct(int slot) { return slot>=0 && slot<=40; }
}
