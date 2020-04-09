package com.xxmicloxx.blockoverlay.mixins;

import com.xxmicloxx.blockoverlay.ContainerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ContainerPropertyUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenContainerS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class OpenContainerMixin {
    @Inject(method = "onOpenContainer", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
    ))
    private void onOpenContainerTrap(OpenContainerS2CPacket packet, CallbackInfo ci) {
        if (ContainerHelper.INSTANCE.getCurrentRequest() != null) {
            ContainerHelper.INSTANCE.getCurrentRequest().setSyncId(packet.getSyncId());
            ci.cancel();
        }
    }

    @Inject(method = "onInventory", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
    ))
    private void onInventoryTrap(InventoryS2CPacket packet, CallbackInfo ci) {
        ContainerHelper.ContentRequest request = ContainerHelper.INSTANCE.getCurrentRequest();
        if (request == null) {
            return;
        }

        Integer guiId = request.getSyncId();
        if (guiId == null || guiId != packet.getGuiId()) {
            return;
        }

        ci.cancel();
        ContainerHelper.INSTANCE.gotContainerContents(packet.getSlotStacks());
    }

    @Inject(method = "onContainerPropertyUpdate", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
    ))
    private void onContainerPropertyUpdateTrap(ContainerPropertyUpdateS2CPacket packet, CallbackInfo ci) {
        if (ContainerHelper.INSTANCE.gotContainerProperties(packet.getSyncId(), packet.getPropertyId(), packet.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntityAnimation", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
            shift = At.Shift.AFTER
    ))
    private void onEntityAnimationTrap(EntityAnimationS2CPacket packet, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        if (packet.getId() != MinecraftClient.getInstance().player.getEntityId()) {
            return;
        }

        if (packet.getAnimationId() != 0) { // swing main arm
            return;
        }

        /*if (!ContainerHelper.INSTANCE.getRequesting()) {
            return;
        }*/

        // Nope >:)
        ci.cancel();
    }
}
