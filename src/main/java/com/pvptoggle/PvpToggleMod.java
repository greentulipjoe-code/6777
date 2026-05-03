package com.pvptoggle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PvpToggle - Minecraft 1.21.11 Fabric Mod (Yarn Mappings)
 *
 * Press RIGHT SHIFT to toggle:
 *   ON  -> view bobbing off, hit tilt off, non-vanilla/fullbright packs disabled
 *   OFF -> view bobbing on, hit tilt on
 */
public class PvpToggleMod implements ClientModInitializer {

    public static final String MOD_ID = "pvptoggle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean pvpModeEnabled = false;
    private boolean wasRightShiftPressed = false;

    private static final String[] FULLBRIGHT_KEYWORDS = {
        "fullbright", "full_bright", "full-bright", "gamma", "nightvision", "night_vision"
    };

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PvpToggle] Initialized for Minecraft 1.21.11");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) return;

            // Use InputUtil for key detection - works reliably in Fabric
            boolean isRightShiftPressed = InputUtil.isKeyPressed(
                client.getWindow().getHandle(),
                GLFW.GLFW_KEY_RIGHT_SHIFT
            );

            if (isRightShiftPressed && !wasRightShiftPressed) {
                togglePvpMode(client);
            }

            wasRightShiftPressed = isRightShiftPressed;
        });
    }

    private void togglePvpMode(MinecraftClient client) {
        pvpModeEnabled = !pvpModeEnabled;
        GameOptions options = client.options;

        if (pvpModeEnabled) {
            // PVP MODE ON
            options.getBobView().setValue(false);
            options.getDamageTiltStrength().setValue(false);
            disableNonVanillaPacks(client);
            LOGGER.info("[PvpToggle] PvP Mode ENABLED");
            client.player.sendMessage(
                Text.literal("§a[PvpToggle] §fPvP Mode: §aON §7(bobbing off, hit tilt off, packs reset)"),
                true
            );
        } else {
            // PVP MODE OFF
            options.getBobView().setValue(true);
            options.getDamageTiltStrength().setValue(true);
            LOGGER.info("[PvpToggle] PvP Mode DISABLED");
            client.player.sendMessage(
                Text.literal("§a[PvpToggle] §fPvP Mode: §cOFF §7(bobbing on, hit tilt on)"),
                true
            );
        }

        client.options.write();
    }

    private void disableNonVanillaPacks(MinecraftClient client) {
        ResourcePackManager packManager = client.getResourcePackManager();
        packManager.scanPacks();

        Collection<ResourcePackProfile> enabled = packManager.getEnabledProfiles();
        List<String> toKeep = new ArrayList<>();

        for (ResourcePackProfile profile : enabled) {
            String id = profile.getId();
            String idLower = id.toLowerCase();

            boolean isVanillaOrBuiltin =
                idLower.equals("vanilla")
                || idLower.startsWith("fabric")
                || profile.isPinned();

            boolean isFullbright = containsFullbrightKeyword(idLower);

            if (isVanillaOrBuiltin || isFullbright) {
                toKeep.add(id);
                LOGGER.info("[PvpToggle] Keeping pack: {}", id);
            } else {
                LOGGER.info("[PvpToggle] Disabling pack: {}", id);
            }
        }

        client.options.resourcePacks.clear();
        for (String id : toKeep) {
            ResourcePackProfile p = packManager.getProfile(id);
            if (p != null && !p.isPinned()) {
                client.options.resourcePacks.add(id);
            }
        }

        client.options.write();
        client.reloadResources();
    }

    private boolean containsFullbrightKeyword(String name) {
        for (String kw : FULLBRIGHT_KEYWORDS) {
            if (name.contains(kw)) return true;
        }
        return false;
    }

    public static boolean isPvpModeEnabled() {
        return pvpModeEnabled;
    }
}
