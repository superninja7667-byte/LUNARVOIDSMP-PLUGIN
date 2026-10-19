package gg.lunarvoidsmp;

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Essentials bundles its own Adventure classes; bridge through JSON, never cast across them. */
final class EssentialsMessages {
    private EssentialsMessages() {}
    static String plain(com.earth2me.essentials.libs.kyori.adventure.text.Component message) {
        String json=com.earth2me.essentials.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(message);
        return PlainTextComponentSerializer.plainText().serialize(GsonComponentSerializer.gson().deserialize(json));
    }
}
