package io.github.jd1378.persianfontfixer.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public final class PersianFontFixerBungee extends Plugin {
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new ProxyChatListener());
        getLogger().info("Rewriting chat from clients older than 1.16.2; newer clients render Arabic script themselves.");
    }
}
