package com.speedopt.mixin;

import com.speedopt.SpeedOpt;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercepts ".speedopt ..." chat messages so they never reach the server. */
@Mixin(ClientPacketListener.class)
public class ChatMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void speedopt$chat(String message, CallbackInfo ci) {
        if (SpeedOpt.handle(message)) ci.cancel();
    }
}
