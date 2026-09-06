package io.github.jd1378.persianfontfixer.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import io.github.jd1378.persianfontfixer.rtl.RtlText;

/**
 * Proxy-side fix, legacy design: the sender's message is rewritten once, so every viewer gets
 * the visual form. Only right for networks where all clients are older than 1.16.2; from that
 * version on the client shapes and reorders Arabic script itself (MC-35765) and would reverse
 * the visual form again. Messages from such senders are left alone. The sender's own language
 * is irrelevant: what matters is the viewers', and BungeeCord has no per-viewer chat hook.
 */
public final class ProxyChatListener implements Listener {
    /** Protocol number of Minecraft 1.16.2, the first client that renders Arabic script itself. */
    static final int SELF_RENDERING_PROTOCOL = 751;

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        if (player.getPendingConnection().getVersion() >= SELF_RENDERING_PROTOCOL) {
            return;
        }
        String message = event.getMessage();
        if (RtlText.hasArabic(message)) {
            event.setMessage(RtlText.toVisual(message));
        }
    }
}
