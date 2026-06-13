package com.xton.inventoryswap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Recolors the "[INV]" tag (and profile name) on a sign as soon as a player writes it,
 * so it's obvious at a glance which signs the plugin will respond to.
 */
public class SignStyleListener implements Listener {

    private static final String TAG_LINE = "[inv]";

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getSide() != Side.FRONT) {
            return;
        }

        Component tagLine = event.line(0);
        if (tagLine == null) {
            return;
        }

        String tag = PlainTextComponentSerializer.plainText().serialize(tagLine).trim();
        if (!tag.equalsIgnoreCase(TAG_LINE)) {
            return;
        }

        event.line(0, SignStyle.styledTag());

        Component profileLine = event.line(1);
        if (profileLine != null) {
            String profileName = PlainTextComponentSerializer.plainText().serialize(profileLine).trim();
            if (!profileName.isEmpty()) {
                event.line(1, SignStyle.styledProfileName(profileName));
            }
        }
    }
}
