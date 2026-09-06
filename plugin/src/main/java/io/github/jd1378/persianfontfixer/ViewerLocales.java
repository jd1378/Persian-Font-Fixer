package io.github.jd1378.persianfontfixer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.bukkit.entity.Player;

/** Decides, per player, whether the client already renders right-to-left text on its own. */
final class ViewerLocales {
    /** Resolved once: Player#getLocale on 1.12+, Player.Spigot#getLocale via reflection before that. */
    private static final Method LEGACY_GET_LOCALE = resolveLegacyGetLocale();

    private final Set<String> rtlLanguages = new HashSet<String>();

    ViewerLocales(Collection<String> rtlLanguages) {
        for (String language : rtlLanguages) {
            this.rtlLanguages.add(languageOf(language));
        }
    }

    private static Method resolveLegacyGetLocale() {
        try {
            Player.class.getMethod("getLocale");
            return null;
        } catch (NoSuchMethodException pre112) {
            try {
                return Player.Spigot.class.getMethod("getLocale");
            } catch (NoSuchMethodException none) {
                return null;
            }
        }
    }

    /** True when the player's client does its own shaping and reordering, so the text must stay untouched. */
    boolean rendersRtl(Player player) {
        String locale = localeOf(player);
        return locale != null && rtlLanguages.contains(languageOf(locale));
    }

    static String languageOf(String locale) {
        int cut = locale.indexOf('_');
        String language = cut < 0 ? locale : locale.substring(0, cut);
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private static String localeOf(Player player) {
        if (LEGACY_GET_LOCALE == null) {
            return player.getLocale();
        }
        try {
            return (String) LEGACY_GET_LOCALE.invoke(player.spigot());
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
