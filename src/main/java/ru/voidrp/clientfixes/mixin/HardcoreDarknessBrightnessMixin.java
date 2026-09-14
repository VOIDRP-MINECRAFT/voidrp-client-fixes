package ru.voidrp.clientfixes.mixin;

import com.hyrrx.hardcoretruedarkness.client.BrightnessLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.voidrp.clientfixes.darkness.DarknessTuning;

/**
 * Replaces Hardcore True Darkness's brightness lock (gamma forced to 0% every client tick)
 * with a ceiling: players may use the brightness slider up to {@link DarknessTuning#maxGamma()}.
 */
@Mixin(value = BrightnessLock.class, remap = false)
public abstract class HardcoreDarknessBrightnessMixin {

    @Inject(method = "enforce", at = @At("HEAD"), cancellable = true)
    private static void voidrp$capInsteadOfLock(CallbackInfo ci) {
        ci.cancel();
        Options options = Minecraft.getInstance().options;
        if (options == null) return;
        Double current = options.gamma().get();
        double max = DarknessTuning.maxGamma();
        if (current != null && current > max + 1.0E-4) {
            options.gamma().set(max);
        }
    }
}
