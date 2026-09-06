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
        Viewers viewers = new Viewers(
                new ClientVersions(getConfig().getString("assume-clients", "auto"), getLogger()),
                new ViewerLocales(getConfig().getStringList("rtl-locales")));

        String mode = getConfig().getString("listener", "auto").toLowerCase(Locale.ROOT);
        boolean paperApi = hasClass(PAPER_CHAT_EVENT);
        if (mode.equals("paper") && !paperApi) {
            getLogger().severe("listener: paper requested but this server has no Paper chat API; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // The Paper listener class is only loaded after the check, so Spigot never sees its imports.
        // Adventure differs across the Paper builds this plugin supports, so prove the transform works first.
        boolean paper = !mode.equals("bukkit") && paperApi;
        if (paper && !PaperChatListener.selfTest()) {
            if (mode.equals("paper")) {
                getLogger().severe("listener: paper requested but this server's Adventure cannot run the transform; disabling.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().warning("This server's Adventure cannot run the per-viewer transform; using the Bukkit chat event instead.");
            paper = false;
        }

        Logger debug = getConfig().getBoolean("debug", false) ? getLogger() : null;
        Listener listener = paper ? new PaperChatListener(viewers, getLogger(), debug) : new BukkitChatListener(viewers, debug);
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
