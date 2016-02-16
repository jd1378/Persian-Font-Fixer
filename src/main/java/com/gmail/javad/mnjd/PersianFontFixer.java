package com.gmail.javad.mnjd;

import net.md_5.bungee.api.plugin.Plugin;

public class PersianFontFixer extends Plugin {
	
	@Override
    public void onEnable() {
		getProxy().getPluginManager().registerListener(this, new PlayerChatListener());
    }

}