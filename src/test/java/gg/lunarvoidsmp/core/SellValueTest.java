package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SellValueTest {
    @Test void capAppliesToEachItemAndWholeStacksCanExceedThirtyThousand() {
        assertEquals(3_000_000,SellValue.stack(Money.parse("30000"),1));
        assertEquals(192_000_000,SellValue.stack(Money.parse("30000"),64));
        assertEquals(1_000_001,SellValue.stack(Money.parse("10000.01"),1));
        assertThrows(IllegalArgumentException.class,()->SellValue.stack(Money.parse("30000.01"),1));
    }
    @Test void preservesCentsAndRejectsNonpositivePricesOrCounts() {
        assertEquals(192,SellValue.stack(Money.parse("0.03"),64));
        assertThrows(IllegalArgumentException.class,()->SellValue.stack(0,64));
        assertThrows(IllegalArgumentException.class,()->SellValue.stack(-1,64));
        assertThrows(IllegalArgumentException.class,()->SellValue.stack(100,0));
        assertThrows(IllegalArgumentException.class,()->SellValue.stack(100,-1));
    }
    @Test void auctionAmountsKeepTheirSeparateExistingLimits() {
        assertEquals(5_000_000,Money.parse("50000"));
    }
    @Test void slotMappingExcludesCraftingAndEveryOtherContainersOwnSlots() {
        for(int slot=-1;slot<=46;slot++) assertEquals(slot>=5 && slot<=45,InventoryPriceSlots.content(0,46,slot));
        // Double chest occupies 0..53, player inventory occupies 54..89.
        for(int slot=-1;slot<=90;slot++) assertEquals(slot>=54 && slot<90,InventoryPriceSlots.content(7,90,slot));
        // Furnace occupies 0..2, player inventory occupies 3..38.
        assertFalse(InventoryPriceSlots.content(8,39,2));
        assertTrue(InventoryPriceSlots.content(8,39,3));
        assertFalse(InventoryPriceSlots.content(-1,46,5));
        assertFalse(InventoryPriceSlots.content(0,47,5));
        assertFalse(InventoryPriceSlots.content(7,0,0));
        assertTrue(InventoryPriceSlots.direct(0)); assertTrue(InventoryPriceSlots.direct(40));
        assertFalse(InventoryPriceSlots.direct(-1)); assertFalse(InventoryPriceSlots.direct(41));
    }
}
