package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path directory;
    Store store;
    Bank bank;
    TradeService service;
    static class Bank implements TradeService.Economy {
        final Map<String,Long> balances=new HashMap<>(Map.of("buyer",100_00L,"seller",0L));
        int withdraws,deposits;
        boolean rejectSeller,rejectRefund,throwDeposit;
        public boolean has(String owner,long amount) { return balances.getOrDefault(owner,0L)>=amount; }
        public boolean withdraw(String owner,long amount) { withdraws++; if(!has(owner,amount)) return false; balances.merge(owner,-amount,Long::sum); return true; }
        public boolean deposit(String owner,long amount) {
            deposits++;
            if(throwDeposit) throw new IllegalStateException("provider unavailable; result unknown");
            if((owner.equals("seller")&&rejectSeller)||(owner.equals("buyer")&&rejectRefund)) return false;
            balances.merge(owner,amount,Long::sum); return true;
        }
    }
    @BeforeEach void start() throws Exception { store=new Store(directory.resolve("state.db")); bank=new Bank(); service=new TradeService(store,bank); }
    @AfterEach void stop() throws Exception { if(store!=null) store.close(); }
    Store.Trade pay(String id) { return new Store.Trade(id,"PAY","buyer","seller",25_00,0,"PREPARED",null); }
    String item(String owner,String purpose) throws SQLException { return store.addItem(owner,new byte[]{1,2,3},"HELD",purpose); }
    @Test void parsesCentsWithoutFloatingPointOrExponent() {
        assertEquals(250050,Money.parse("2500.50")); assertEquals(1,Money.parse("0.01"));
        for(String bad:List.of("-1","0","NaN","Infinity","1e3","1.001","1,000"," ","1000000000000","+1")) assertThrows(IllegalArgumentException.class,()->Money.parse(bad),bad);
        assertEquals("2.0K",Money.shortAmount(200000));
    }
    @Test void ranksExactPartialAndTypoMatches() {
        assertTrue(Search.rank("diamond","DIAMOND")<Search.rank("di","DIAMOND"));
        assertTrue(Search.rank("di","DIAMOND")<Search.rank("diamnd","DIAMOND"));
        assertTrue(Search.rank("diamnd","DIAMOND SWORD")>=0);
        assertEquals(-1,Search.rank("zz","DIAMOND")); assertEquals(-1,Search.rank("netherite","DIAMOND"));
    }
    @Test void repeatedPaymentIsChargedAndCreditedOnce() throws Exception {
        assertTrue(service.execute(pay("same"),List.of())); assertFalse(service.execute(pay("same"),List.of()));
        assertEquals(7500,bank.balances.get("buyer")); assertEquals(2500,bank.balances.get("seller")); assertEquals(1,bank.withdraws);
    }
    @Test void insufficientFundsMoveNothing() throws Exception {
        bank.balances.put("buyer",0L); assertFalse(service.execute(pay("low"),List.of()));
        assertEquals(0,bank.withdraws); assertEquals(0,bank.deposits); assertEquals("FAILED",store.trade("low").state());
    }
    @Test void failedRecipientGetsOneRefund() throws Exception {
        bank.rejectSeller=true; assertFalse(service.execute(pay("refund"),List.of()));
        assertEquals(10000,bank.balances.get("buyer")); assertEquals(0,bank.balances.get("seller"));
        assertEquals(1,bank.withdraws); assertEquals(2,bank.deposits); assertEquals("FAILED",store.trade("refund").state());
    }
    @Test void failedRefundRequiresReviewAndCannotRetry() throws Exception {
        bank.rejectSeller=true; bank.rejectRefund=true;
        assertFalse(service.execute(pay("review"),List.of())); assertEquals("REVIEW",store.trade("review").state());
        assertFalse(service.execute(pay("review"),List.of())); assertEquals(1,bank.withdraws);
    }
    @Test void uncertainProviderExceptionDoesNotGuessARefund() throws Exception {
        bank.throwDeposit=true; assertThrows(IllegalStateException.class,()->service.execute(pay("unknown"),List.of()));
        assertEquals("REVIEW",store.trade("unknown").state()); assertEquals(1,bank.deposits); assertEquals(7500,bank.balances.get("buyer"));
    }
    @Test void databaseFailureAfterDebitQuarantinesTransaction() throws Exception {
        store.update("CREATE TRIGGER fail_after_debit BEFORE UPDATE OF state ON trades WHEN NEW.state='WITHDRAWN' BEGIN SELECT RAISE(ABORT,'simulated disk failure'); END");
        assertThrows(SQLException.class,()->service.execute(pay("disk"),List.of()));
        assertEquals("REVIEW",store.trade("disk").state()); assertEquals(7500,bank.balances.get("buyer")); assertEquals(0,bank.deposits);
    }
    @Test void selfPaymentsAndInvalidAmountsRejectedBeforeSideEffects() {
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("self","PAY","buyer","buyer",100,0,"PREPARED",null),List.of()));
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("neg","PAY","buyer","seller",-100,0,"PREPARED",null),List.of()));
        assertEquals(0,bank.withdraws);
    }
    @Test void sellConsumesOnlyReservedItemsOnce() throws Exception {
        String a=item("seller","SELL"),b=item("seller","SELL"),untouched=item("seller","CLAIM");
        Store.Trade t=new Store.Trade("sale","SELL",null,"seller",1500,0,"PREPARED",null);
        assertTrue(service.execute(t,List.of(a,b))); assertFalse(service.execute(t,List.of(a,b)));
        assertEquals("CONSUMED",store.item(a).state()); assertEquals("CONSUMED",store.item(b).state()); assertEquals("HELD",store.item(untouched).state()); assertEquals(1500,bank.balances.get("seller"));
    }
    @Test void failedSalePreservesItems() throws Exception {
        String a=item("seller","SELL"); bank.rejectSeller=true;
        assertFalse(service.execute(new Store.Trade("sale","SELL",null,"seller",1500,0,"PREPARED",null),List.of(a)));
        assertEquals("HELD",store.item(a).state()); assertEquals("SELL",store.item(a).purpose());
    }
    @Test void stockPurchaseCreatesClaimOnlyAfterSuccessfulDebit() throws Exception {
        String a=item("buyer","STOCK");
        assertTrue(service.execute(new Store.Trade("stock","BUY_STOCK","buyer",null,1000,0,"PREPARED",null),List.of(a)));
        assertEquals("CLAIM",store.item(a).purpose()); assertEquals(9000,bank.balances.get("buyer"));
    }
    @Test void failedStockPurchaseVoidsGeneratedItems() throws Exception {
        String a=item("buyer","STOCK"); bank.balances.put("buyer",0L);
        assertFalse(service.execute(new Store.Trade("stock","BUY_STOCK","buyer",null,1000,0,"PREPARED",null),List.of(a)));
        assertEquals("VOID",store.item(a).state());
    }
    @Test void resourcesRollbackTogetherWhenOneItemAlreadyMoved() throws Exception {
        String a=item("seller","SELL"),b=item("seller","CLAIM");
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("bad","SELL",null,"seller",1500,0,"PREPARED",null),List.of(a,b)));
        assertEquals("HELD",store.item(a).state()); assertNull(store.trade("bad")); assertEquals(0,bank.deposits);
    }
    @Test void auctionPaysOfflineSellerAndTransfersWholeEscrowOnce() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,2);
        Store.Trade t=new Store.Trade("auction","BUY_AUCTION","buyer","seller",2500,250,"PREPARED",listing);
        assertTrue(service.execute(t,List.of(a))); assertEquals(2250,bank.balances.get("seller"));
        assertEquals("buyer",store.item(a).owner()); assertEquals("CLAIM",store.item(a).purpose()); assertTrue(store.listings().isEmpty());
        Store.Trade other=new Store.Trade("auction2","BUY_AUCTION","buyer","seller",2500,250,"PREPARED",listing);
        assertThrows(IllegalArgumentException.class,()->service.execute(other,List.of(a))); assertEquals(1,bank.withdraws);
    }
    @Test void auctionRejectsSubstitutedPriceSellerOrItem() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,2),other=item("seller","AUCTION");
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("price","BUY_AUCTION","buyer","seller",1,0,"PREPARED",listing),List.of(a)));
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("seller","BUY_AUCTION","buyer","other",2500,0,"PREPARED",listing),List.of(a)));
        assertThrows(IllegalArgumentException.class,()->service.execute(new Store.Trade("item","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing),List.of(other)));
        assertEquals(1,store.listings().size()); assertEquals(0,bank.withdraws);
    }
    @Test void failedAuctionPaymentReleasesListingWithoutTransferringItem() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,2); bank.rejectSeller=true;
        assertFalse(service.execute(new Store.Trade("auction","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing),List.of(a)));
        assertEquals("seller",store.item(a).owner()); assertEquals("AUCTION",store.item(a).purpose()); assertEquals(1,store.listings().size()); assertEquals(10000,bank.balances.get("buyer"));
    }
    @Test void listingLimitsAndCancellationRespectOwner() throws Exception {
        String a=item("seller","CLAIM"),b=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        assertThrows(IllegalArgumentException.class,()->store.list(b,"seller",1000,1));
        assertFalse(store.cancel(listing,"buyer")); assertTrue(store.cancel(listing,"seller")); assertFalse(store.cancel(listing,"seller"));
        assertEquals("CLAIM",store.item(a).purpose()); assertEquals("CLAIM",store.item(b).purpose());
    }
    @Test void activeListingsHaveNoDeadlineEvenWithLegacyTimestampsAfterRestart() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        store.update("UPDATE listings SET created=0,expires=1");
        store.close(); store=new Store(directory.resolve("state.db")); store.recover(); service=new TradeService(store,bank);
        assertEquals(listing,store.listings().getFirst().id());
        assertEquals("AUCTION",store.item(a).purpose()); assertEquals("HELD",store.item(a).state()); assertEquals(0,bank.deposits);
        String other=item("seller","CLAIM");
        assertThrows(IllegalArgumentException.class,()->store.list(other,"seller",100,1));
        assertTrue(service.buyAuction(new Store.Trade("old-listing","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing),a,()->true));
        assertTrue(store.listings().isEmpty()); assertEquals("buyer",store.item(a).owner());
    }
    @Test void fullInventoryLeavesTheListingAndMoneyUntouchedUntilBuyerMakesRoom() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        Store.Trade trade=new Store.Trade("inventory","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing);
        assertThrows(IllegalArgumentException.class,()->service.buyAuction(trade,a,()->false));
        assertNull(store.trade(trade.id())); assertEquals(0,bank.withdraws); assertEquals(0,bank.deposits);
        assertEquals(10000,bank.balances.get("buyer")); assertEquals(0,bank.balances.get("seller"));
        assertEquals("seller",store.item(a).owner()); assertEquals("HELD",store.item(a).state()); assertEquals("AUCTION",store.item(a).purpose());
        assertEquals(listing,store.listings().getFirst().id());
        assertTrue(service.buyAuction(trade,a,()->true)); assertEquals(1,bank.withdraws);
        assertEquals("buyer",store.item(a).owner()); assertTrue(store.listings().isEmpty());
    }
    @Test void sellersCannotPurchaseTheirOwnListingEvenByBypassingTheMenu() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        Store.Trade trade=new Store.Trade("self-auction","BUY_AUCTION","seller","seller",2500,0,"PREPARED",listing);
        assertThrows(IllegalArgumentException.class,()->service.buyAuction(trade,a,()->true));
        assertThrows(IllegalArgumentException.class,()->store.begin(trade,List.of(a)));
        assertNull(store.trade(trade.id())); assertEquals(0,bank.withdraws); assertEquals(0,bank.deposits);
        assertEquals(listing,store.listings().getFirst().id()); assertEquals("seller",store.item(a).owner());
    }
    @Test void completedSaleStaysRemovedAndOwnedByBuyerAcrossRestart() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        assertTrue(service.buyAuction(new Store.Trade("sold","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing),a,()->true));
        store.close(); store=new Store(directory.resolve("state.db")); store.recover();
        assertTrue(store.listings().isEmpty()); assertEquals("buyer",store.item(a).owner()); assertEquals("CLAIM",store.item(a).purpose());
        assertEquals("COMPLETE",store.trade("sold").state());
    }
    @Test void previouslyExpiredItemsAreNotRelistedAfterTheyBecameClaims() throws Exception {
        String a=item("seller","CLAIM"); store.list(a,"seller",2500,1);
        store.update("UPDATE listings SET state='EXPIRED'"); store.update("UPDATE items SET purpose='CLAIM' WHERE id=?",a);
        store.close(); store=new Store(directory.resolve("state.db")); store.recover();
        assertTrue(store.listings().isEmpty()); assertEquals("CLAIM",store.item(a).purpose()); assertEquals("seller",store.item(a).owner());
    }
    @Test void durableRestartPreservesPreferencesAndQuarantinesUncertainWork() throws Exception {
        store.setting("buyer","global_chat",false);
        String safe=item("buyer","SELL"),moving=store.addItem("buyer",new byte[]{4},"REMOVING","CLAIM"),delivering=store.addItem("buyer",new byte[]{5},"DELIVERING","CLAIM");
        store.begin(pay("pending"),List.of()); store.stage("pending","WITHDRAWING");
        store.close(); store=new Store(directory.resolve("state.db")); store.recover();
        assertEquals(false,store.setting("buyer","global_chat")); assertEquals("CLAIM",store.item(safe).purpose());
        assertEquals("REVIEW",store.item(moving).state()); assertEquals("REVIEW",store.item(delivering).state()); assertEquals("REVIEW",store.trade("pending").state());
        assertFalse(new TradeService(store,bank).execute(pay("pending"),List.of())); assertEquals(0,bank.withdraws);
    }
    @Test void uncertainAuctionRestartDoesNotExposeListingOrReleaseItems() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        Store.Trade t=new Store.Trade("pending","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing);
        assertTrue(store.begin(t,List.of(a))); store.recover();
        assertTrue(store.listings().isEmpty()); assertEquals("REVIEW",store.item(a).state()); assertFalse(store.cancel(listing,"seller"));
        assertTrue(store.reviews().stream().anyMatch(s->s.contains(a)));
    }
    @Test void reconciledCompletedAuctionTransfersEscrowWithoutRetryingMoney() throws Exception {
        String a=item("seller","CLAIM"),listing=store.list(a,"seller",2500,1);
        Store.Trade t=new Store.Trade("pending","BUY_AUCTION","buyer","seller",2500,0,"PREPARED",listing);
        store.begin(t,List.of(a)); store.recover(); store.reconcileTrade(t.id(),true,"operator","external economy logs confirm both effects");
        assertEquals("COMPLETE",store.trade(t.id()).state()); assertEquals("buyer",store.item(a).owner());
        assertEquals("CLAIM",store.item(a).purpose()); assertEquals("HELD",store.item(a).state()); assertEquals(0,bank.withdraws);
        assertThrows(IllegalArgumentException.class,()->store.reconcileTrade(t.id(),true,"operator","repeat"));
    }
    @Test void reconciliationFailureRollsBackWhenReasonIsMissing() throws Exception {
        store.begin(pay("review"),List.of()); store.recover();
        assertThrows(IllegalArgumentException.class,()->store.reconcileTrade("review",false,"operator",""));
        assertEquals("REVIEW",store.trade("review").state());
    }
    @Test void reconciledItemReturnIsClaimableOnlyOnce() throws Exception {
        String a=store.addItem("seller",new byte[]{4},"REMOVING","CLAIM"); store.recover();
        store.reconcileItem(a,true,"operator","verified item missing from player inventory");
        assertEquals("HELD",store.item(a).state()); assertEquals("CLAIM",store.item(a).purpose());
        assertThrows(IllegalArgumentException.class,()->store.reconcileItem(a,true,"operator","repeat"));
        assertTrue(store.itemState(a,"HELD","DELIVERING")); assertFalse(store.itemState(a,"HELD","DELIVERING"));
    }
    @Test void itemCannotBypassItsUncertainMoneyTransaction() throws Exception {
        String a=item("seller","SELL"); Store.Trade t=new Store.Trade("pending","SELL",null,"seller",100,0,"PREPARED",null);
        store.begin(t,List.of(a)); store.recover();
        assertThrows(IllegalArgumentException.class,()->store.reconcileItem(a,true,"operator","cannot bypass money journal"));
        assertEquals("REVIEW",store.item(a).state());
    }
}
