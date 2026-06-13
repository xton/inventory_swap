package com.xton.inventoryswap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Shared visual styling for "[INV]" signs, so the sign-edit and swap-trigger
 * listeners always agree on what a "registered" sign looks like.
 */
final class SignStyle {

    private SignStyle() {
    }

    static Component styledTag() {
        return Component.text("[inv]", NamedTextColor.GRAY, TextDecoration.ITALIC);
    }

    static Component styledLoadoutName(String loadoutName) {
        return Component.text(loadoutName, NamedTextColor.DARK_GREEN, TextDecoration.BOLD);
    }
}
