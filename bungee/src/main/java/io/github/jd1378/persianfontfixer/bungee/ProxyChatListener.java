package io.github.jd1378.persianfontfixer.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import io.github.jd1378.persianfontfixer.rtl.RtlText;

/**
 * Proxy-side fix, legacy design: the sender's message is rewritten once, so every viewer
 * (including clients that render right-to-left text themselves) gets the visual form.
 * BungeeCord has no per-viewer chat hook, and silently drops changes to 1.19.1+ signed chat.
 */
public final class ProxyChatListener implements Listener {
    /** Protocol version of Minecraft 1.19.1, the first with signed chat the proxy cannot alter. */
    static final int SIGNED_CHAT_PROTOCOL = 760;

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        if (player.getPendingConnection().getVersion() >= SIGNED_CHAT_PROTOCOL) {
            return;
        }
        String message = event.getMessage();
        if (RtlText.hasArabic(message)) {
            event.setMessage(RtlText.toVisual(message));
        }
    }
}
