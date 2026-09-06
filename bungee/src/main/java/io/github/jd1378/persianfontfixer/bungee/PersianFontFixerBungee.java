package io.github.jd1378.persianfontfixer.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public final class PersianFontFixerBungee extends Plugin {
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new ProxyChatListener());
        getLogger().info("Rewriting chat for clients up to 1.19; 1.19.1+ signed chat cannot be changed at the proxy.");
    }
}
