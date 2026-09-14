package ru.voidrp.clientfixes.mixin;

import com.hyrrx.hardcoretruedarkness.client.LightmapDarkener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ru.voidrp.clientfixes.darkness.DarknessTuning;

/**
 * Softens Hardcore True Darkness's lightmap rewrite.
 *
 * <p>{@code LightmapDarkener.darkenPixel} scales every lightmap colour by
 * {@code lerp(1, 0.002, darkness)} — at full darkness only 0.2% of the light is left —
 * and {@code applyDayUnlitDarkness} darkens unlit spots by 72% even at noon. Both constants
 * are swapped for {@link DarknessTuning} values.
 */
@Mixin(value = LightmapDarkener.class, remap = false)
public abstract class HardcoreDarknessLightmapMixin {

    @ModifyConstant(method = "darkenPixel", constant = @Constant(floatValue = 0.002F))
    private static float voidrp$softenFullDarkness(float original) {
        return DarknessTuning.lightmapFloor();
    }

    @ModifyConstant(method = "applyDayUnlitDarkness", constant = @Constant(floatValue = 0.72F))
    private static float voidrp$softenUnlitAmbient(float original) {
        return DarknessTuning.unlitAmbientDarkness();
    }
}
