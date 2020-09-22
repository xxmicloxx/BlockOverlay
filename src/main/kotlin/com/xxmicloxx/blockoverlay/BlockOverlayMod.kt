package com.xxmicloxx.blockoverlay

import com.xxmicloxx.blockoverlay.render.OverlayRenderer
import com.xxmicloxx.blockoverlay.render.Textures
import com.xxmicloxx.blockoverlay.render.TransparentBufferBuilder
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding
import net.fabricmc.fabric.api.event.client.ClientTickCallback
import net.minecraft.util.Util

// For support join https://discord.gg/v6v4pMv

private var lastRender: Long? = null

@Suppress("unused")
fun init() {
    KeyBindings.init()

    ClientTickCallback.EVENT.register(ClientTickCallback { tick() })
}

fun renderInit() {
    TransparentBufferBuilder.renderInit()
    Textures.renderInit()
}

fun tick() {
    KeyBindings.tick()
    ContainerHelper.tick()
    OverlayRenderer.tick()
}

fun render() {
    val myLastRender = lastRender
    val currentTime = Util.getMeasuringTimeNano()
    val delta = if (myLastRender == null) {
        0f
    } else {
        (currentTime - myLastRender) / 1.0e9f
    }
    lastRender = currentTime

    OverlayRenderer.render(delta)
}