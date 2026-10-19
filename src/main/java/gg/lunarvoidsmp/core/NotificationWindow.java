package gg.lunarvoidsmp.core;

import java.util.*;

/** Exact, recipient-specific request prompts; unrelated chat is never broadly muted. */
public final class NotificationWindow {
    private record Prompt(String text,long expires) {}
    private final Map<UUID,List<Prompt>> prompts=new HashMap<>();
    private static final long LIFETIME_MILLIS=1000;

    public synchronized void prepare(UUID owner,Collection<String> messages,long now) {
        expire(now);
        List<Prompt> list=prompts.computeIfAbsent(owner,id->new ArrayList<>());
        for(String message:messages) if(message!=null && !message.isBlank()) list.add(new Prompt(message,now+LIFETIME_MILLIS));
    }
    public synchronized boolean consume(UUID owner,String message,long now) {
        List<Prompt> list=prompts.get(owner);
        if(list==null) return false;
        boolean matched=false;
        for(Iterator<Prompt> iterator=list.iterator();iterator.hasNext();) {
            Prompt prompt=iterator.next();
            if(prompt.expires()<=now) { iterator.remove(); continue; }
            if(!matched && prompt.text().equals(message)) { iterator.remove(); matched=true; }
        }
        if(list.isEmpty()) prompts.remove(owner);
        return matched;
    }
    public synchronized void expire(long now) {
        prompts.values().forEach(list->list.removeIf(prompt->prompt.expires()<=now));
        prompts.values().removeIf(List::isEmpty);
    }
    public synchronized void forget(UUID owner) { prompts.remove(owner); }
    public synchronized void clear() { prompts.clear(); }
}
