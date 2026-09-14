package ru.voidrp.clientfixes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.voidrp.clientfixes.darkness.DarknessTuning;

/**
 * VoidRP Client Fixes — a client-only home for crash guards / compatibility patches
 * against buggy client-side behaviour in the VoidRP (FTB Evolution) modpack.
 *
 * This mod is NOT required on the server: its mods.toml deps are CLIENT-side and it
 * declares displayTest=IGNORE_ALL, so a client carrying it can join a server that does
 * not have it without any mod-list mismatch.
 *
 * Current fixes:
 *  - {@link ru.voidrp.clientfixes.mixin.TipsyChatCrashGuardMixin}: stops Brewin' And
 *    Chewin''s Tipsy chat handler from crashing the client (and disconnecting everyone)
 *    when an incoming chat message is not a vanilla 2-argument chat.type.text component.
 *  - {@code HardcoreDarkness*Mixin}: soften Hardcore True Darkness to a light dimming
 *    (see {@link ru.voidrp.clientfixes.darkness.DarknessTuning}).
 */
@Mod(value = VoidRpClientFixes.MODID, dist = Dist.CLIENT)
public final class VoidRpClientFixes {

    public static final String MODID = "voidrp_client_fixes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public VoidRpClientFixes(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DarknessTuning.SPEC);
        LOGGER.info("[VoidRP Client Fixes] loaded (client-only compatibility patches)");
    }
}
