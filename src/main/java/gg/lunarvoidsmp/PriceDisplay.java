package gg.lunarvoidsmp;

import java.util.UUID;

/** Boundary for a recipient-specific inventory display adapter. */
interface PriceDisplay extends AutoCloseable {
    boolean healthy();
    void forget(UUID player);
    @Override void close();
}
