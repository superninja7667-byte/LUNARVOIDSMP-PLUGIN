package gg.lunarvoidsmp.core;

import java.util.*;
import java.util.function.Consumer;

/** Accept each eligible request once; providers retain their permission, expiry and teleport checks. */
public final class AutoTpa {
    public record Request(UUID requester,long created,String name,boolean here,boolean online) {}
    private record Key(UUID requester,long created) {}
    private final Map<UUID,Set<Key>> attempted=new HashMap<>();

    public void accept(UUID target,boolean enabled,List<Request> requests,Consumer<String> acceptCommand) {
        if(!enabled) { forget(target); return; }
        Set<Key> seen=attempted.computeIfAbsent(target,id->new HashSet<>());
        Set<Key> current=new HashSet<>();
        for(Request request:requests) current.add(new Key(request.requester(),request.created()));
        seen.retainAll(current);
        for(Request request:requests) {
            Key key=new Key(request.requester(),request.created());
            if(request.here() || !request.online() || !seen.add(key)) continue;
            acceptCommand.accept(request.name());
        }
    }
    public void forget(UUID target) { attempted.remove(target); }
}
