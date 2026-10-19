package gg.lunarvoidsmp;

import java.util.List;
import java.util.UUID;

interface RequestNotifications extends AutoCloseable {
    boolean healthy();
    void prepare(UUID player,List<String> messages);
    void forget(UUID player);
    void expire();
    @Override void close();
}
