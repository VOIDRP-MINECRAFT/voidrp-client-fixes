package ru.voidrp.clientfixes.darkness;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * How much of Hardcore True Darkness survives on the client.
 *
 * <p>The mod turns an unlit or night scene pitch black: its lightmap multiplies colours
 * down to 0.2%, even daytime unlit corners lose 72%, a final post-shader crushes what is
 * left, and the brightness slider is locked to 0%. Players voted 50/50 on removing the mod
 * (2026-09), so the decision was to keep it as a light dimming only. The mixins in
 * {@code ru.voidrp.clientfixes.mixin.HardcoreDarkness*} feed these values into those four places.
 *
 * <p>File: {@code config/voidrp_client_fixes-client.toml}. The launcher ships it with
 * alwaysOverwrite, so the values are tuned centrally and stay the same for every player.
 */
public final class DarknessTuning {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue LIGHTMAP_FLOOR;
    private static final ModConfigSpec.DoubleValue UNLIT_AMBIENT_DARKNESS;
    private static final ModConfigSpec.DoubleValue POST_PASS_STRENGTH;
    private static final ModConfigSpec.DoubleValue MAX_GAMMA;

    // Defaults: full darkness keeps ~45% of the light, daytime unlit corners lose ~20%.
    static final double DEFAULT_LIGHTMAP_FLOOR = 0.45;
    static final double DEFAULT_UNLIT_AMBIENT_DARKNESS = 0.2;
    static final double DEFAULT_POST_PASS_STRENGTH = 0.3;
    static final double DEFAULT_MAX_GAMMA = 0.5;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Softening of Hardcore True Darkness (VoidRP). Shipped by the launcher — local edits are overwritten.")
                .push("hardcore_true_darkness");
        LIGHTMAP_FLOOR = builder
                .comment("Brightness kept at full darkness, 0..1. The original mod uses 0.002 (pitch black); 1 = no darkening.")
                .defineInRange("lightmapFloor", DEFAULT_LIGHTMAP_FLOOR, 0.002, 1.0);
        UNLIT_AMBIENT_DARKNESS = builder
                .comment("Darkening of spots with no block/sky light even in daytime, 0..1. Original: 0.72.")
                .defineInRange("unlitAmbientDarkness", DEFAULT_UNLIT_AMBIENT_DARKNESS, 0.0, 0.72);
        POST_PASS_STRENGTH = builder
                .comment("Share of the final darkness post-shader that is applied, 0..1. Original: 1 (full crush).")
                .defineInRange("postPassStrength", DEFAULT_POST_PASS_STRENGTH, 0.0, 1.0);
        MAX_GAMMA = builder
                .comment("Highest brightness (gamma) the player may set, 0..1. The original mod locks it to 0 (Moody).")
                .defineInRange("maxGamma", DEFAULT_MAX_GAMMA, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private DarknessTuning() {
    }

    // Guarded reads: the darkness code runs from the first client tick and render frame,
    // possibly before the config file is loaded — reading an unloaded ModConfigSpec throws
    // ("Cannot get config value before config is loaded") and would crash the client.
    public static float lightmapFloor() {
        return (float) (SPEC.isLoaded() ? LIGHTMAP_FLOOR.get() : DEFAULT_LIGHTMAP_FLOOR);
    }

    public static float unlitAmbientDarkness() {
        return (float) (SPEC.isLoaded() ? UNLIT_AMBIENT_DARKNESS.get() : DEFAULT_UNLIT_AMBIENT_DARKNESS);
    }

    public static float postPassStrength() {
        return (float) (SPEC.isLoaded() ? POST_PASS_STRENGTH.get() : DEFAULT_POST_PASS_STRENGTH);
    }

    public static double maxGamma() {
        return SPEC.isLoaded() ? MAX_GAMMA.get() : DEFAULT_MAX_GAMMA;
    }
}
