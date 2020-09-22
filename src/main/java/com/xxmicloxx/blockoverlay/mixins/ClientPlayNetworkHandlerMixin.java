package com.xxmicloxx.blockoverlay.mixins;

import com.xxmicloxx.blockoverlay.ContainerHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void sendPacketTrap(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) {
            // pass these
            return;
        }

        PlayerInteractBlockC2SPacket interactPacket = (PlayerInteractBlockC2SPacket) packet;

        if (ContainerHelper.INSTANCE.handlePlayerInteract(interactPacket)) {
            ci.cancel();
        }
    }
}
