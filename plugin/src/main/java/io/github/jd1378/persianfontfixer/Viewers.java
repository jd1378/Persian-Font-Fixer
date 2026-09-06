package io.github.jd1378.persianfontfixer;

import org.bukkit.entity.Player;

/** Decides, per player, whether the chat line must be sent in the visual form. */
final class Viewers {
    private final ClientVersions versions;
    private final ViewerLocales locales;

    Viewers(ClientVersions versions, ViewerLocales locales) {
        this.versions = versions;
        this.locales = locales;
    }

    boolean needsVisualForm(Player player) {
        return !versions.rendersArabicItself(player) && !locales.rendersRtl(player);
    }
}
