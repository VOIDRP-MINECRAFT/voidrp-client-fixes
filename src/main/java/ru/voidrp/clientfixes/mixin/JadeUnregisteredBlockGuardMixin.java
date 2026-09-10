package ru.voidrp.clientfixes.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import snownee.jade.api.BlockAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.voidrp.clientfixes.VoidRpClientFixes;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stops Jade from disconnecting the client when it looks at a block that has no
 * network id.
 *
 * <p>Crash (client, Netty IO thread — the player is thrown to the server list with
 * "Соединение потеряно"), reproduced 2026-09-10 21:03 by casting a Weapons of
 * Miracles ground-smash skill:
 *
 * <pre>
 *   io.netty.handler.codec.EncoderException: Failed to encode packet
 *       'serverbound/minecraft:custom_payload'
 *   Caused by: Failed encoding custom payload jade:request_block
 *   Caused by: IllegalArgumentException: Can't find id for
 *       'Block{epicfight:fracture_block}'
 *     at net.minecraft.core.IdMap.getIdOrThrow
 * </pre>
 *
 * <p>Root cause: Epic Fight's terrain-fracture effect (the cracked ground left by a
 * heavy weapon skill, spawned via its {@code SPCreateTerrainFracture} packet) puts a
 * {@code FractureBlock} in the world that is not present in the synced block registry
 * — it has no network id. Jade's crosshair tooltip does not care what it is looking
 * at: it packs the {@link BlockState} into {@code jade:request_block} and asks the
 * server for extra data. Encoding that block throws, netty reports an encoder failure
 * on the connection, and vanilla drops the connection. So merely *looking at* the
 * cracks a skill just made kicks the player.
 *
 * <p>Fix: skip the request when the block has no id. Jade simply shows what it can
 * derive client-side for that one block, which is the correct degradation — the
 * alternative is being disconnected. Everything else keeps working untouched.
 *
 * <p>This is a client-side-only symptom: the server never sees the packet (it fails
 * during encoding, before it is sent) and stays perfectly healthy.
 */
@Mixin(targets = "snownee.jade.util.ClientProxy", remap = false)
public abstract class JadeUnregisteredBlockGuardMixin {

    private static final AtomicLong voidrp_lastWarn = new AtomicLong(0L);

    // The first parameter MUST be declared as the real BlockAccessor type: a mixin
    // injector whose descriptor does not match its target silently fails to apply, and
    // with defaultRequire 0 it fails without a word — the guard would look deployed while
    // doing nothing. (Same trap cost us three rounds on the server-side goal-set crash.)
    @Inject(method = "requestBlockData", at = @At("HEAD"), cancellable = true, require = 0)
    private static void voidrp_skipUnregisteredBlock(
            BlockAccessor accessor, List<?> providers, CallbackInfo ci) {
        try {
            if (accessor == null) {
                return;
            }
            BlockState state = accessor.getBlockState();
            if (state == null) {
                return;
            }
            Block block = state.getBlock();

            // Check BOTH id maps. The packet encodes the BLOCK STATE, and on NeoForge
            // states live in their own map (BlockCallbacks.BLOCKSTATE_TO_ID_MAP, exposed
            // as Block.BLOCK_STATE_REGISTRY) which is separate from the block registry.
            // The first version of this guard only checked the block registry — where
            // epicfight:fracture_block IS present — so it never fired and the client kept
            // getting disconnected (2026-09-10 21:19). Epic Fight builds its fracture
            // states through a custom FractureBlockState that never enters the state map.
            boolean stateKnown = Block.BLOCK_STATE_REGISTRY.getId(state) >= 0;
            boolean blockKnown = BuiltInRegistries.BLOCK.getId(block) >= 0;
            if (stateKnown && blockKnown) {
                return;
            }

            long now = System.currentTimeMillis();
            long last = voidrp_lastWarn.get();
            if (now - last >= 30_000L && voidrp_lastWarn.compareAndSet(last, now)) {
                VoidRpClientFixes.LOGGER.warn(
                        "[VoidRP] Jade block-data request skipped for {} (stateKnown={}, blockKnown={}) — no network id, "
                                + "so encoding jade:request_block would throw and disconnect us "
                                + "(Epic Fight's fracture_block does this). Tooltip details for this "
                                + "block are unavailable; everything else is unaffected. "
                                + "Further occurrences suppressed for 30 s.",
                        block, stateKnown, blockKnown);
            }
            ci.cancel();
        } catch (Throwable guardFailure) {
            // A guard must never be the reason something breaks — fall through to Jade.
        }
    }
}
