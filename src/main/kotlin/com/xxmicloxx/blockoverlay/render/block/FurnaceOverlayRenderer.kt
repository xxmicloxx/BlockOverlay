package com.xxmicloxx.blockoverlay.render.block

import com.xxmicloxx.blockoverlay.render.DrawCache
import com.xxmicloxx.blockoverlay.render.Textures
import com.xxmicloxx.blockoverlay.render.bridge.RenderBridge
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import com.xxmicloxx.blockoverlay.render.state.ContainerRenderState
import com.xxmicloxx.blockoverlay.render.state.FurnaceRenderState
import net.minecraft.block.AbstractFurnaceBlock
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.FurnaceBlockEntity

class FurnaceOverlayRenderer : ContainerOverlayRenderer() {
    override val matchedBlocks: Set<BlockEntityType<out BlockEntity>>
        get() = setOf(BlockEntityType.FURNACE, BlockEntityType.BLAST_FURNACE, BlockEntityType.SMOKER)

    override fun createRenderState(entity: BlockEntity): BlockRenderState<BlockEntity> =
        FurnaceRenderState(entity as AbstractFurnaceBlockEntity)

    override fun drawContent(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge) {
        val furnaceState = state as FurnaceRenderState

        if (bridge.globalAlpha != 1f) {
            // cannot use cache
            redrawContent(state, bridge)
            return
        }

        if (furnaceState.pollUpdate()) {
            // clear caches
            furnaceState.clearCache()
        }

        val cache = furnaceState.getCache(bridge.lightUv)
        if (!cache.isCached) {
            cache.startDrawing(true)
            redrawContent(state, bridge)
            cache.finishDrawing()
        } else {
            // draw from cache
            cache.draw()
        }
    }

    private fun redrawContent(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge) {
        bridge.itemFrame(state.inventory[0]).pos(0.15, 0.15).z(0.01).draw()
        bridge.itemFrame(state.inventory[1]).pos(0.15, 0.65).z(0.01).draw()
        bridge.itemFrame(state.inventory[2]).pos(0.5, 0.35).z(0.01).height(0.3).draw()

        val isLit = state.entity.cachedState.get(AbstractFurnaceBlock.LIT)
        val builder = bridge.rect(0.21, 0.4275, 0.15, 0.15).texture(Textures.FLAMES).z(0.01)
        if (isLit) {
            builder.color(1f, 0.9f, 0f, 0.9f).draw()
        } else {
            builder.color(0.05f, 0.05f, 0.05f, 0.9f).draw()
        }
    }
}