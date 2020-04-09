package com.xxmicloxx.blockoverlay

import com.xxmicloxx.blockoverlay.render.OverlayRenderer
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.util.concurrent.TimeUnit

object KeyBindings {
    private const val KEY_HOLD_THRESHOLD = 250L

    private lateinit var toggleOverlay: FabricKeyBinding
    private var toggleOverlayHoldStart: Long? = null
    private var toggleOverlayWasOn: Boolean = false

    fun init() {
        KeyBindingRegistry.INSTANCE.addCategory("Block Overlay")

        toggleOverlay = FabricKeyBinding.Builder.create(
            Identifier("blockoverlay", "toggle"),
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "Block Overlay"
        ).build()

        KeyBindingRegistry.INSTANCE.register(toggleOverlay)
    }

    fun tick() {
        checkToggleOverlay()
    }

    private fun checkToggleOverlay() {
        val holdStart = toggleOverlayHoldStart
        if (toggleOverlay.isPressed && holdStart == null) {
            // rising edge!
            toggleOverlayHoldStart = Util.getMeasuringTimeMs()
            toggleOverlayWasOn = OverlayRenderer.currentState.enabled
            if (!toggleOverlayWasOn) {
                OverlayRenderer.enable()
            }
        } else if (!toggleOverlay.isPressed && holdStart != null) {
            // no longer holding, reset
            toggleOverlayHoldStart = null

            // check elapsed time
            val elapsed = Util.getMeasuringTimeMs() - holdStart
            if ((elapsed >= KEY_HOLD_THRESHOLD && !toggleOverlayWasOn) ||
                (elapsed < KEY_HOLD_THRESHOLD && toggleOverlayWasOn)) {

                // turn off again
                OverlayRenderer.disable()
            }
        }
    }
}