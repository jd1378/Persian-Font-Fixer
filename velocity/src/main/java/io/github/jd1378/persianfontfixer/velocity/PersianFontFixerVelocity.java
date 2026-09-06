package io.github.jd1378.persianfontfixer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

import io.github.jd1378.persianfontfixer.rtl.RtlText;

/**
 * Proxy-side fix, legacy design: the sender's message is rewritten once, so every viewer gets
 * the visual form. Only right for networks where all clients are older than 1.16.2; from that
 * version on the client shapes and reorders Arabic script itself (MC-35765) and would reverse
 * the visual form again. Messages from such senders are left alone. The sender's own language
 * is irrelevant: what matters is the viewers', and Velocity has no per-viewer chat hook. The
 * guard also keeps clear of 1.19.1+ signed chat, which Velocity disconnects a player for
 * changing (SessionChatHandler#invalidChange).
 */
@Plugin(id = "persianfontfixer", name = "PersianFontFixer", version = "2.1.0", authors = {"Javad Mnjd"},
        description = "Shapes and reorders Persian and Arabic chat at the proxy for clients up to 1.19. Do not combine with the backend plugin.")
public final class PersianFontFixerVelocity {
    private final Logger logger;

    @Inject
    public PersianFontFixerVelocity(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        logger.info("Rewriting chat from clients older than 1.16.2; newer clients render Arabic script themselves.");
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()
                || event.getPlayer().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_16_2) >= 0) {
            return;
        }
        String message = event.getMessage();
        if (RtlText.hasArabic(message)) {
            event.setResult(PlayerChatEvent.ChatResult.message(RtlText.toVisual(message)));
        }
    }
}
