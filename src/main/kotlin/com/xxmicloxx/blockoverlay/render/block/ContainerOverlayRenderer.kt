package com.xxmicloxx.blockoverlay.render.block

import com.mojang.blaze3d.systems.RenderSystem
import com.xxmicloxx.blockoverlay.render.MC
import com.xxmicloxx.blockoverlay.render.Textures
import com.xxmicloxx.blockoverlay.render.bridge.RenderBridge
import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import com.xxmicloxx.blockoverlay.render.state.ContainerRenderState
import net.minecraft.block.entity.BlockEntity
import org.lwjgl.opengl.GL11

abstract class ContainerOverlayRenderer : BlockOverlayRenderer {
    companion object {
        private const val LOADING_SIZE = 0.75
        private const val ERROR_SIZE = 0.50
    }

    protected open fun startRender(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge) {
        bridge.move(0.05)
        bridge.drawBackground()
    }

    protected open fun getContainerStatus(state: ContainerRenderState<BlockEntity>): ContainerRenderState.Status =
            state.status


    override fun render(state: BlockRenderState<BlockEntity>, bridge: RenderBridge) {
        val containerState = state as ContainerRenderState<BlockEntity>

        startRender(containerState, bridge)

        when (getContainerStatus(containerState)) {
            ContainerRenderState.Status.LOADING -> drawLoader(bridge)
            ContainerRenderState.Status.FAILED -> drawError(bridge)
            ContainerRenderState.Status.LOADED -> drawContent(containerState, bridge)
        }
    }

    private fun drawLoader(bridge: RenderBridge) {
        val offset = (1 - LOADING_SIZE) / 2
        bridge.rect(offset, offset, LOADING_SIZE, LOADING_SIZE).texture(Textures.LOADER).draw()
    }

    private fun drawError(bridge: RenderBridge) {
        val offset = (1 - ERROR_SIZE) / 2
        bridge.rect(offset, offset, ERROR_SIZE, ERROR_SIZE).texture(Textures.ERROR).draw()
    }

    protected abstract fun drawContent(state: ContainerRenderState<BlockEntity>, bridge: RenderBridge)
}