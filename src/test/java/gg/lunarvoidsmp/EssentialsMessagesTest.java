package gg.lunarvoidsmp;

import com.earth2me.essentials.libs.kyori.adventure.text.Component;
import com.earth2me.essentials.libs.kyori.adventure.text.format.NamedTextColor;
import com.earth2me.essentials.libs.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EssentialsMessagesTest {
    @Test void bridgesEssentialsBundledAdventureToPaperWithoutLosingRequestText() {
        var message=Component.text(".Bedrock_Name",NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
            .append(Component.text(" has requested to teleport to you."));
        assertEquals(".Bedrock_Name has requested to teleport to you.",EssentialsMessages.plain(message));
    }
    @Test void retainsCustomLanguageAndMultilineContent() {
        assertEquals("Téléportation 玩家\n/tpaccept",EssentialsMessages.plain(Component.text("Téléportation 玩家\n/tpaccept")));
    }
}
