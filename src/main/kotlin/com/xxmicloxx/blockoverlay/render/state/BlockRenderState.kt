package com.xxmicloxx.blockoverlay.render.state

import net.minecraft.block.entity.BlockEntity

abstract class BlockRenderState<out T : BlockEntity>(val entity: T) {
    init {
        println("Creating render state at ${entity.pos}")
    }

    open fun destroy() {
        println("Destroying render state at ${entity.pos}")
    }
}