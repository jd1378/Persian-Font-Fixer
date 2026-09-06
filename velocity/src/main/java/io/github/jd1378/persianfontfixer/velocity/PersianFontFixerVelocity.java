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
 * Proxy-side fix, legacy design: the sender's message is rewritten once, so every viewer
 * (including clients that render right-to-left text themselves) gets the visual form.
 * Velocity has no per-viewer chat hook, and disconnects the player when a plugin changes a
 * signed 1.19.1+ message (SessionChatHandler#invalidChange), hence the protocol version guard.
 */
@Plugin(id = "persianfontfixer", name = "PersianFontFixer", version = "2.0.0", authors = {"Javad Mnjd"},
        description = "Shapes and reorders Persian and Arabic chat at the proxy for clients up to 1.19. Do not combine with the backend plugin.")
public final class PersianFontFixerVelocity {
    private final Logger logger;

    @Inject
    public PersianFontFixerVelocity(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        logger.info("Rewriting chat for clients up to 1.19; 1.19.1+ signed chat cannot be changed at the proxy.");
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()
                || event.getPlayer().getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
            return;
        }
        String message = event.getMessage();
        if (RtlText.hasArabic(message)) {
            event.setResult(PlayerChatEvent.ChatResult.message(RtlText.toVisual(message)));
        }
    }
}
