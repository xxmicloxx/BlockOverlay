package com.xxmicloxx.blockoverlay.render.state

import com.xxmicloxx.blockoverlay.render.DrawCache
import net.minecraft.block.entity.AbstractFurnaceBlockEntity

class FurnaceRenderState(entity: AbstractFurnaceBlockEntity) : ContainerRenderState<AbstractFurnaceBlockEntity>(entity) {

    private val caches = mutableMapOf<Int, DrawCache>()

    init {
        requestContents()
    }

    fun clearCache() {
        caches.values.forEach { it.destroy() }
    }

    fun getCache(lightUv: Int): DrawCache =
            caches.getOrPut(lightUv) { DrawCache() }

    override fun destroy() {
        clearCache()
        super.destroy()
    }
}