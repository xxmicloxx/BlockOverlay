package com.xxmicloxx.blockoverlay.render.bridge

import net.minecraft.client.render.Tessellator

data class RenderContext(val tess: Tessellator, val lightUv: Int, val globalAlpha: Float, val bridge: RenderBridge)