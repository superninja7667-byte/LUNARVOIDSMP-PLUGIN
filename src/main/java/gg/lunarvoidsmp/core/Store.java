package gg.lunarvoidsmp.core;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** One connection, accessed on the server thread. Every external effect has a durable preceding state. */
public final class Store implements AutoCloseable {
    public record Item(String id, String owner, byte[] payload, String state, String purpose, String tx) {}
    public record Listing(String id, String item, String owner, long price, long created) {}
    public record Trade(String id, String kind, String buyer, String seller, long amount, long fee, String state, String listing) {}
    private final Connection connection;

    public Store(Path file) throws SQLException {
        try { Class.forName("org.sqlite.JDBC"); } catch(ClassNotFoundException e) { throw new SQLException("Bundled SQLite driver unavailable",e); }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement statement=connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("PRAGMA foreign_keys=ON");
        }
        update("CREATE TABLE IF NOT EXISTS settings(owner TEXT, name TEXT, value INTEGER, PRIMARY KEY(owner,name))");
        update("CREATE TABLE IF NOT EXISTS items(id TEXT PRIMARY KEY, owner TEXT NOT NULL, payload BLOB NOT NULL, state TEXT NOT NULL, purpose TEXT NOT NULL, tx TEXT)");
        // Retain the legacy expires column for existing databases; it is never used as a deadline.
        update("CREATE TABLE IF NOT EXISTS listings(id TEXT PRIMARY KEY, item TEXT UNIQUE NOT NULL REFERENCES items(id), owner TEXT NOT NULL, price INTEGER NOT NULL, created INTEGER NOT NULL, expires INTEGER NOT NULL, state TEXT NOT NULL)");
        update("CREATE TABLE IF NOT EXISTS trades(id TEXT PRIMARY KEY, kind TEXT NOT NULL, buyer TEXT, seller TEXT, amount INTEGER NOT NULL, fee INTEGER NOT NULL, state TEXT NOT NULL, listing TEXT)");
        update("CREATE TABLE IF NOT EXISTS reconciliations(id INTEGER PRIMARY KEY, record TEXT NOT NULL, decision TEXT NOT NULL, operator TEXT NOT NULL, reason TEXT NOT NULL, created INTEGER NOT NULL)");
        update("CREATE INDEX IF NOT EXISTS items_owner ON items(owner,state,purpose)");
        update("CREATE INDEX IF NOT EXISTS listings_state ON listings(state,created)");
    }
    public synchronized int update(String sql, Object... args) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(sql)) { bind(p, args); return p.executeUpdate(); }
    }
    private static void bind(PreparedStatement p, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
    }
    private interface Work<T> { T run() throws SQLException; }
    private synchronized <T> T atomic(Work<T> work) throws SQLException {
        connection.setAutoCommit(false);
        try { T value = work.run(); connection.commit(); return value; }
        catch (SQLException | RuntimeException e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(true); }
    }
    public synchronized Boolean setting(String owner, String name) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("SELECT value FROM settings WHERE owner=? AND name=?")) {
            bind(p, new Object[]{owner,name}); try (ResultSet r = p.executeQuery()) { return r.next() ? r.getBoolean(1) : null; }
        }
    }
    public void setting(String owner, String name, boolean value) throws SQLException {
        update("INSERT INTO settings VALUES(?,?,?) ON CONFLICT(owner,name) DO UPDATE SET value=excluded.value",owner,name,value);
    }
    public String addItem(String owner, byte[] bytes, String state, String purpose) throws SQLException {
        String id = UUID.randomUUID().toString();
        update("INSERT INTO items VALUES(?,?,?,?,?,NULL)",id,owner,bytes,state,purpose); return id;
    }
    public synchronized List<Item> items(String owner) throws SQLException {
        List<Item> out = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM items WHERE owner=? AND state NOT IN ('DELIVERED','CONSUMED','VOID') ORDER BY rowid")) {
            p.setString(1,owner); try (ResultSet r = p.executeQuery()) { while(r.next()) out.add(item(r)); }
        }
        return out;
    }
    public synchronized Item item(String id) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM items WHERE id=?")) {
            p.setString(1,id); try (ResultSet r = p.executeQuery()) { return r.next() ? item(r) : null; }
        }
    }
    private Item item(ResultSet r) throws SQLException { return new Item(r.getString("id"),r.getString("owner"),r.getBytes("payload"),r.getString("state"),r.getString("purpose"),r.getString("tx")); }
    public boolean itemState(String id, String from, String to) throws SQLException { return update("UPDATE items SET state=? WHERE id=? AND state=?",to,id,from)==1; }
    public synchronized Trade trade(String id) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM trades WHERE id=?")) {
            p.setString(1,id); try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Trade(id,r.getString("kind"),r.getString("buyer"),r.getString("seller"),r.getLong("amount"),r.getLong("fee"),r.getString("state"),r.getString("listing")) : null;
            }
        }
    }
    public void stage(String id, String state) throws SQLException { update("UPDATE trades SET state=? WHERE id=?",state,id); }
    public boolean begin(Trade t, List<String> itemIds) throws SQLException {
        return atomic(() -> {
            if (trade(t.id()) != null) return false;
            if (t.listing()!=null && update("UPDATE listings SET state='RESERVED' WHERE id=? AND state='ACTIVE' AND owner<>? AND owner=? AND price=? AND item=?",t.listing(),t.buyer(),t.seller(),t.amount(),itemIds.getFirst())!=1)
                throw new IllegalArgumentException("This listing is no longer available.");
            update("INSERT INTO trades VALUES(?,?,?,?,?,?,?,?)",t.id(),t.kind(),t.buyer(),t.seller(),t.amount(),t.fee(),"PREPARED",t.listing());
            for (String id : itemIds) {
                String owner = "BUY_AUCTION".equals(t.kind()) ? t.seller() : t.buyer()!=null ? t.buyer() : t.seller();
                String purpose = switch(t.kind()) { case "SELL" -> "SELL"; case "BUY_STOCK" -> "STOCK"; case "BUY_AUCTION" -> "AUCTION"; default -> throw new IllegalArgumentException("Invalid item transaction"); };
                if(update("UPDATE items SET state='RESERVED',tx=? WHERE id=? AND owner=? AND state='HELD' AND purpose=?",t.id(),id,owner,purpose)!=1)
                    throw new IllegalArgumentException("An item has already moved. Reopen the menu.");
            }
            return true;
        });
    }
    public void finish(Trade t, boolean success) throws SQLException {
        atomic(() -> {
            finishRows(t,success); return null;
        });
    }
    private void finishRows(Trade t,boolean success) throws SQLException {
            if(success) {
                if("SELL".equals(t.kind())) update("UPDATE items SET state='CONSUMED' WHERE tx=? AND state='RESERVED'",t.id());
                else update("UPDATE items SET state='HELD',purpose='CLAIM',owner=? WHERE tx=? AND state='RESERVED'",t.buyer(),t.id());
                if(t.listing()!=null) update("UPDATE listings SET state='SOLD' WHERE id=?",t.listing());
                stage(t.id(),"COMPLETE");
            } else {
                update("UPDATE items SET state=?,tx=NULL WHERE tx=? AND state='RESERVED'","BUY_STOCK".equals(t.kind())?"VOID":"HELD",t.id());
                if(t.listing()!=null) update("UPDATE listings SET state='ACTIVE' WHERE id=? AND state='RESERVED'",t.listing());
                stage(t.id(),"FAILED");
            }
    }
    public void reconcileTrade(String id,boolean complete,String operator,String reason) throws SQLException {
        atomic(() -> {
            Trade t=trade(id);
            if(t==null || !t.state().equals("REVIEW")) throw new IllegalArgumentException("Only a REVIEW transaction can be reconciled.");
            update("UPDATE items SET state='RESERVED' WHERE tx=? AND state='REVIEW'",id);
            finishRows(t,complete); audit(id,complete?"complete":"failed",operator,reason); return null;
        });
    }
    public void reconcileItem(String id,boolean returnItem,String operator,String reason) throws SQLException {
        atomic(() -> {
            Item i=item(id);
            if(i==null || !Set.of("REVIEW","REMOVING","DELIVERING").contains(i.state())) throw new IllegalArgumentException("Item does not require reconciliation.");
            if(i.tx()!=null) {
                Trade t=trade(i.tx());
                if(t!=null && !Set.of("COMPLETE","FAILED").contains(t.state())) throw new IllegalArgumentException("Reconcile the related transaction first: "+i.tx());
            }
            update("UPDATE items SET state=?,purpose='CLAIM' WHERE id=?",returnItem?"HELD":"DELIVERED",id);
            audit(id,returnItem?"return":"delivered",operator,reason); return null;
        });
    }
    private void audit(String id,String decision,String operator,String reason) throws SQLException {
        if(reason==null || reason.isBlank()) throw new IllegalArgumentException("A reconciliation reason is required.");
        update("INSERT INTO reconciliations(record,decision,operator,reason,created) VALUES(?,?,?,?,?)",id,decision,operator,reason,System.currentTimeMillis());
    }
    public String list(String itemId, String owner, long price, int limit) throws SQLException {
        if(price<=0 || limit<=0) throw new IllegalArgumentException("Invalid listing rules.");
        return atomic(() -> {
            long now=System.currentTimeMillis();
            try (PreparedStatement p=connection.prepareStatement("SELECT count(*) FROM listings WHERE owner=? AND state IN ('ACTIVE','RESERVED')")) {
                p.setString(1,owner); try(ResultSet r=p.executeQuery()) { if(r.next() && r.getInt(1)>=limit) throw new IllegalArgumentException("Listing limit reached."); }
            }
            if(update("UPDATE items SET purpose='AUCTION' WHERE id=? AND owner=? AND state='HELD' AND purpose='CLAIM'",itemId,owner)!=1) throw new IllegalArgumentException("Item is unavailable.");
            String id=UUID.randomUUID().toString();
            update("INSERT INTO listings VALUES(?,?,?,?,?,?,?)",id,itemId,owner,price,now,0,"ACTIVE"); return id;
        });
    }
    public synchronized List<Listing> listings() throws SQLException {
        List<Listing> out=new ArrayList<>();
        try(PreparedStatement p=connection.prepareStatement("SELECT * FROM listings WHERE state='ACTIVE' ORDER BY created DESC")) {
            try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new Listing(r.getString("id"),r.getString("item"),r.getString("owner"),r.getLong("price"),r.getLong("created"))); }
        }
        return out;
    }
    public boolean cancel(String id,String owner) throws SQLException {
        return atomic(() -> {
            if(update("UPDATE listings SET state='CANCELLED' WHERE id=? AND owner=? AND state='ACTIVE'",id,owner)!=1) return false;
            update("UPDATE items SET purpose='CLAIM' WHERE id=(SELECT item FROM listings WHERE id=?)",id); return true;
        });
    }
    public void recover() throws SQLException {
        atomic(() -> {
            update("UPDATE trades SET state='REVIEW' WHERE state NOT IN ('COMPLETE','FAILED')");
            update("UPDATE items SET state='REVIEW' WHERE state IN ('REMOVING','DELIVERING','RESERVED')");
            update("UPDATE items SET purpose='CLAIM' WHERE state='HELD' AND purpose='SELL'");
            update("UPDATE items SET state='VOID' WHERE state='HELD' AND purpose='STOCK'"); return null;
        });
    }
    public synchronized List<String> reviews() throws SQLException {
        List<String> out=new ArrayList<>();
        try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT 'transaction '||id||' '||kind||' amount-cents='||amount||' buyer='||coalesce(buyer,'')||' seller='||coalesce(seller,'') FROM trades WHERE state='REVIEW' UNION ALL SELECT 'item '||id||' owner='||owner||' purpose='||purpose FROM items WHERE state IN ('REVIEW','REMOVING','DELIVERING') OR (state='RESERVED' AND tx IN (SELECT id FROM trades WHERE state='REVIEW')) LIMIT 100")) { while(r.next()) out.add(r.getString(1)); }
        return out;
    }
    @Override public synchronized void close() throws SQLException { connection.close(); }
}
