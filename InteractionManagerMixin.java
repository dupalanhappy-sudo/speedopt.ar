package com.speedopt.mixin;

import com.speedopt.SpeedOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class InteractionManagerMixin {

    @Unique private long speedopt$lastCrystal = -1000;
    @Unique private long speedopt$lastAnchor = -1000;
    @Unique private boolean speedopt$anchorExploding = false;

    /* ---------------- CRYSTAL OPTIMIZER ---------------- */

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void speedopt$crystalHead(Player player, Entity target, CallbackInfo ci) {
        if (!SpeedOpt.config.enabled || !SpeedOpt.config.crystalEnabled) return;
        if (!(target instanceof EndCrystal)) return;

        // Speed level: ignore hits that come faster than the chosen delay
        long now = SpeedOpt.now();
        if (now - speedopt$lastCrystal < SpeedOpt.config.crystalDelay()) {
            ci.cancel();
            return;
        }
        speedopt$lastCrystal = now;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void speedopt$crystalTail(Player player, Entity target, CallbackInfo ci) {
        if (!SpeedOpt.config.enabled || !SpeedOpt.config.crystalEnabled) return;
        if (!(target instanceof EndCrystal)) return;

        // Remove the crystal on the client immediately so the next one can be placed
        // without waiting for the server's removal packet.
        target.remove(Entity.RemovalReason.KILLED);
    }

    /* ---------------- ANCHOR OPTIMIZER ---------------- */

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void speedopt$anchorHead(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        speedopt$anchorExploding = false;
        if (!SpeedOpt.config.enabled || !SpeedOpt.config.anchorEnabled) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) return;
        if (state.getValue(RespawnAnchorBlock.CHARGE) <= 0) return;
        if (player.getItemInHand(hand).is(Items.GLOWSTONE)) return; // charging, not exploding
        if (level.dimension() == Level.NETHER) return;              // anchors only work in the Nether, explode elsewhere

        long now = SpeedOpt.now();
        if (now - speedopt$lastAnchor < SpeedOpt.config.anchorDelay()) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        speedopt$lastAnchor = now;
        speedopt$anchorExploding = true;
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void speedopt$anchorReturn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (!speedopt$anchorExploding) return;
        speedopt$anchorExploding = false;
        if (!cir.getReturnValue().consumesAction()) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        // Clear the anchor on the client immediately so the next one can be placed instantly.
        level.setBlock(hit.getBlockPos(), Blocks.AIR.defaultBlockState(), 11);
    }
}
