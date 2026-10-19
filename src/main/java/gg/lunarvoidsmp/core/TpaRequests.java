package gg.lunarvoidsmp.core;

import java.util.*;

/** Main-thread request state. A sender can have one outstanding request. */
public final class TpaRequests {
    public record Request(UUID sender,UUID target,boolean here,long expires) {}
    private final Map<UUID,Request> pending=new LinkedHashMap<>();
    private final Map<UUID,Long> last=new HashMap<>();
    public Request create(UUID sender,UUID target,boolean here,long now) {
        last.values().removeIf(time->now-time>=30_000);
        if(sender.equals(target)) throw new IllegalArgumentException("You cannot teleport to yourself.");
        if(last.containsKey(sender) && now-last.get(sender)<30_000) throw new IllegalArgumentException("Please wait 30 seconds between requests.");
        Request request=new Request(sender,target,here,now+60_000); pending.put(sender,request); last.put(sender,now); return request;
    }
    public List<Request> incoming(UUID target,long now) {
        pending.values().removeIf(r->r.expires()<=now);
        return pending.values().stream().filter(r->r.target().equals(target)).toList();
    }
    public Request remove(UUID sender) { return pending.remove(sender); }
    public void forget(UUID id) { pending.values().removeIf(r->r.sender().equals(id)||r.target().equals(id)); }
}
