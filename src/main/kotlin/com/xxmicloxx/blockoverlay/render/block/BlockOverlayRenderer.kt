package com.xxmicloxx.blockoverlay.render.block

import com.xxmicloxx.blockoverlay.render.state.BlockRenderState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.Direction

interface BlockOverlayRenderer {
    val matchedBlocks: Set<BlockEntityType<out BlockEntity>>

    fun render(
        state: BlockRenderState<BlockEntity>,
        side: Direction,
        alpha: Float
    )

    fun createRenderState(entity: BlockEntity): BlockRenderState<BlockEntity>
}