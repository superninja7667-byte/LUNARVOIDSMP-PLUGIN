package gg.lunarvoidsmp.core;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class TradeService {
    public interface Economy {
        boolean has(String owner,long cents);
        boolean withdraw(String owner,long cents);
        boolean deposit(String owner,long cents);
    }
    private final Store store;
    private final Economy economy;
    public TradeService(Store store,Economy economy) { this.store=store; this.economy=economy; }

    /** Run the recipient's current capacity check before reserving a listing or moving money. */
    public boolean buyAuction(Store.Trade trade,String item,BooleanSupplier hasInventorySpace) throws SQLException {
        if(!"BUY_AUCTION".equals(trade.kind())) throw new IllegalArgumentException("Expected an auction purchase.");
        if(!hasInventorySpace.getAsBoolean()) throw new IllegalArgumentException("Make room in your inventory for the whole stack before buying. No money was taken.");
        return execute(trade,List.of(item));
    }

    /** The stable confirmation ID and database reservations make repeated confirmations harmless. */
    public boolean execute(Store.Trade t,List<String> items) throws SQLException {
        if(t.amount()<=0 || t.fee()<0 || t.fee()>=t.amount() || t.amount()>99_999_999_999_999L)
            throw new IllegalArgumentException("Invalid transaction amount.");
        if(t.buyer()!=null && t.buyer().equals(t.seller())) throw new IllegalArgumentException("You cannot pay or buy from yourself.");
        boolean valid=switch(t.kind()) {
            case "PAY" -> t.buyer()!=null && t.seller()!=null && items.isEmpty() && t.listing()==null && t.fee()==0;
            case "SELL" -> t.buyer()==null && t.seller()!=null && !items.isEmpty() && t.listing()==null && t.fee()==0;
            case "BUY_STOCK" -> t.buyer()!=null && t.seller()==null && !items.isEmpty() && t.listing()==null && t.fee()==0;
            case "BUY_AUCTION" -> t.buyer()!=null && t.seller()!=null && items.size()==1 && t.listing()!=null;
            default -> false;
        };
        if(!valid || new java.util.HashSet<>(items).size()!=items.size()) throw new IllegalArgumentException("Invalid transaction resources.");
        if(!store.begin(t,items)) return false;
        try {
            if(t.buyer()!=null) {
                if(!economy.has(t.buyer(),t.amount())) { store.finish(t,false); return false; }
                store.stage(t.id(),"WITHDRAWING");
                if(!economy.withdraw(t.buyer(),t.amount())) { store.finish(t,false); return false; }
                store.stage(t.id(),"WITHDRAWN");
            }
            if(t.seller()!=null) {
                store.stage(t.id(),"CREDITING");
                if(!economy.deposit(t.seller(),t.amount()-t.fee())) {
                    if(t.buyer()!=null) {
                        store.stage(t.id(),"REFUNDING");
                        if(!economy.deposit(t.buyer(),t.amount())) { store.stage(t.id(),"REVIEW"); return false; }
                    }
                    store.finish(t,false); return false;
                }
                store.stage(t.id(),"PAID");
            }
            store.finish(t,true); return true;
        } catch (RuntimeException | SQLException e) {
            // Vault exceptions may happen after a side effect. Never guess whether to retry/refund.
            try { store.stage(t.id(),"REVIEW"); } catch(SQLException journalFailure) { e.addSuppressed(journalFailure); }
            throw e;
        }
    }
}
