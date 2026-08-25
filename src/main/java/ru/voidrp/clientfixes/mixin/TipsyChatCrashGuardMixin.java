package ru.voidrp.clientfixes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.voidrp.clientfixes.VoidRpClientFixes;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stops Brewin' And Chewin''s "Tipsy" chat handler from crashing the client.
 *
 * Crash (client, Render thread — disconnects the player, and because it fires for
 * EVERY received chat message on EVERY client it effectively kicks everyone whenever
 * someone talks):
 *
 *   java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
 *     at umpaz.brewinandchewin.neoforge.client.TipsyEffects.getChatMessage(TipsyEffects.java:50)
 *     at umpaz.brewinandchewin.neoforge.client.TipsyEffects.iCanHear(TipsyEffects.java:28)
 *     at ...ClientHooks.onClientPlayerChat -> ClientboundPlayerChatPacket.handle
 *
 * Root cause: {@code iCanHear} calls {@code getChatMessage(event.getMessage())}
 * UNCONDITIONALLY at the top (before any tipsy/effect check). {@code getChatMessage}
 * assumes the incoming message is a vanilla {@code chat.type.text} TranslatableContents
 * with 2 args {@code [sender, content]} and reads {@code getArgs()[1]}. When the server
 * formats chat into a component that is not a 2-arg translatable (e.g. a plugin-formatted
 * chat message — EssentialsChat / custom format — yielding a single arg or a plain-text
 * component), {@code args[1]} is out of bounds (or the cast fails) → the whole packet
 * handler throws → client disconnects.
 *
 * Fix: guard {@code getChatMessage} at HEAD. If the message is not a TranslatableContents
 * with at least 2 args, return it unchanged (the tipsy garble simply can't apply to a
 * non-standard chat component) instead of crashing. Well-formed vanilla chat is untouched,
 * so the Tipsy effect keeps working normally.
 *
 * Client-only, and brewinandchewin is an optional dependency — if the target is absent
 * the mixin just doesn't apply (require=0).
 */
@Mixin(targets = "umpaz.brewinandchewin.neoforge.client.TipsyEffects", remap = false)
public abstract class TipsyChatCrashGuardMixin {

    private static final AtomicLong voidrp$lastWarn = new AtomicLong(0L);

    @Inject(
        method = "getChatMessage(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void voidrp$guardTipsyChat(Component message, CallbackInfoReturnable<Component> cir) {
        if (message == null) {
            cir.setReturnValue(message);
            return;
        }
        ComponentContents contents = message.getContents();
        boolean safe = contents instanceof TranslatableContents tc && tc.getArgs().length >= 2;
        if (!safe) {
            long now = System.currentTimeMillis();
            long last = voidrp$lastWarn.get();
            if (now - last >= 30_000L && voidrp$lastWarn.compareAndSet(last, now)) {
                VoidRpClientFixes.LOGGER.warn(
                    "[VoidRP Client Fixes] Tipsy chat guard — incoming chat message is not a vanilla " +
                    "2-arg chat.type.text (contents: {}); returning it unchanged to avoid the " +
                    "brewinandchewin getChatMessage crash. Further occurrences suppressed for 30 s.",
                    contents == null ? "null" : contents.getClass().getName());
            }
            cir.setReturnValue(message);
        }
    }
}
