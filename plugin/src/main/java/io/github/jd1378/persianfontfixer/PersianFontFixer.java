package io.github.jd1378.persianfontfixer;

import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class PersianFontFixer extends JavaPlugin {
    private static final String PAPER_CHAT_EVENT = "io.papermc.paper.event.player.AsyncChatEvent";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ViewerLocales locales = new ViewerLocales(getConfig().getStringList("rtl-locales"));

        String mode = getConfig().getString("listener", "auto").toLowerCase(Locale.ROOT);
        boolean paper = mode.equals("paper") || (mode.equals("auto") && hasClass(PAPER_CHAT_EVENT));
        if (paper && !hasClass(PAPER_CHAT_EVENT)) {
            getLogger().severe("listener: paper requested but this server has no Paper chat API; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // The Paper listener class is only loaded after the check, so Spigot never sees its imports.
        Logger debug = getConfig().getBoolean("debug", false) ? getLogger() : null;
        Listener listener = paper ? new PaperChatListener(locales, debug) : new BukkitChatListener(locales, debug);
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Using " + (paper ? "Paper per-viewer renderer" : "Bukkit chat event"));
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
