package ru.voidrp.clientfixes.mixin;

import com.hyrrx.hardcoretruedarkness.client.FinalDarknessPostProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ru.voidrp.clientfixes.darkness.DarknessTuning;

/**
 * Softens Hardcore True Darkness's final screen pass.
 *
 * <p>After the world is drawn the mod runs a post-shader that crushes every dim pixel towards
 * black ({@code DarknessStrength} uniform = darkness intensity), or — when that shader can't
 * load — paints a black overlay with alpha {@code 230 * intensity}. Both are scaled by
 * {@link DarknessTuning#postPassStrength()}.
 */
@Mixin(value = FinalDarknessPostProcessor.class, remap = false)
public abstract class HardcoreDarknessPostPassMixin {

    // First AbstractUniform.set(float) in updateShaderUniforms is DarknessStrength; the second is DaylightPreserve.
    @ModifyArg(
            method = "updateShaderUniforms",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V", ordinal = 0, remap = false),
            index = 0)
    private static float voidrp$softenPostStrength(float intensity) {
        return intensity * DarknessTuning.postPassStrength();
    }

    @ModifyConstant(method = "renderFallbackBlackout", constant = @Constant(floatValue = 230.0F))
    private static float voidrp$softenFallbackBlackout(float original) {
        return original * DarknessTuning.postPassStrength();
    }
}
