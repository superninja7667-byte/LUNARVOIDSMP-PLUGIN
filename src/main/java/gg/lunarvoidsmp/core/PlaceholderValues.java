package gg.lunarvoidsmp.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable snapshots collected on the server thread; placeholder readers never query live providers. */
public final class PlaceholderValues {
    public static final List<String> KEYS=List.of("name","uuid","balance","balance_formatted","balance_short",
        "ping","online","tps","held_sell_value","held_sell_value_formatted","held_sellable");
    public record PlayerValues(String name,Long balanceCents,int ping,Long heldSellCents) {}
    private record ServerValues(Integer online,String tps) {}
    private final Map<UUID,PlayerValues> players=new ConcurrentHashMap<>();
    private volatile ServerValues server=new ServerValues(null,"");
    public void player(UUID id,PlayerValues values) { players.put(id,values); }
    public void server(int online,double tps) {
        String formatted=Double.isFinite(tps) && tps>=0
            ?BigDecimal.valueOf(Math.min(20,tps)).setScale(2,RoundingMode.HALF_UP).toPlainString():"";
        server=new ServerValues(online,formatted);
    }
    public boolean contains(UUID id) { return id!=null && players.containsKey(id); }
    public void forget(UUID id) { players.remove(id); }
    public void clear() { players.clear(); server=new ServerValues(null,""); }
    public static Long cents(double balance) {
        if(!Double.isFinite(balance)) return null;
        try { return BigDecimal.valueOf(balance).movePointRight(2).setScale(0,RoundingMode.HALF_UP).longValueExact(); }
        catch(ArithmeticException e) { return null; }
    }
    private static String numeric(Long cents) { return cents==null?"":BigDecimal.valueOf(cents,2).toPlainString(); }
    private static String formatted(Long cents) { return cents==null?"":String.format(Locale.US,"$%,.2f",BigDecimal.valueOf(cents,2)); }
    public String resolve(UUID id,String key) {
        if(!KEYS.contains(key)) return null;
        if(key.equals("online")) { Integer online=server.online(); return online==null?"":online.toString(); }
        if(key.equals("tps")) return server.tps();
        if(key.equals("uuid")) return id==null?"":id.toString();
        PlayerValues values=id==null?null:players.get(id);
        if(values==null) return "";
        return switch(key) {
            case "name" -> values.name();
            case "balance" -> numeric(values.balanceCents());
            case "balance_formatted" -> formatted(values.balanceCents());
            case "balance_short" -> values.balanceCents()==null?"":Money.shortAmount(values.balanceCents());
            case "ping" -> values.ping()<0?"":Integer.toString(values.ping());
            case "held_sell_value" -> numeric(values.heldSellCents());
            case "held_sell_value_formatted" -> formatted(values.heldSellCents());
            case "held_sellable" -> Boolean.toString(values.heldSellCents()!=null);
            default -> null;
        };
    }
}
