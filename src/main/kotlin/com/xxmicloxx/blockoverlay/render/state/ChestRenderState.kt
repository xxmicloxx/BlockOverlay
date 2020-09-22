package com.xxmicloxx.blockoverlay.render.state

import com.xxmicloxx.blockoverlay.render.DrawCache
import com.xxmicloxx.blockoverlay.render.OverlayRenderer
import com.xxmicloxx.blockoverlay.render.RenderSurface
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

data class ChestRenderInfo(val drawInterleaved: Boolean, val drawSecond: Boolean, val lightUv: Int,
                           val hasSecond: Boolean = true) {
    companion object {
        fun single(lightUv: Int) =
                ChestRenderInfo(drawInterleaved = false, drawSecond = false, hasSecond = false, lightUv = lightUv)
    }
}

class ChestRenderState(entity: ChestBlockEntity) : ContainerRenderState<ChestBlockEntity>(entity) {
    private val isDoubleChest: Boolean
        get() = entity.cachedState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE

    private val otherBlock: BlockPos?
        get() {
            val state = entity.cachedState
            // offset to facing
            return entity.pos.offset(ChestBlock.getFacing(state))
        }

    val otherRenderState: ChestRenderState?
        get() {
            return OverlayRenderer.getRenderState(otherBlock ?: return null) as? ChestRenderState
        }

    var primaryRenderState = this
        private set

    private val caches = mutableMapOf<ChestRenderInfo, DrawCache>()

    init {
        updatePrimaryRenderState()
    }

    private fun isOtherVisible(side: Direction, surfaces: List<RenderSurface>): Boolean {
        val otherPos = otherBlock ?: return false
        return surfaces.any { it.state.entity.pos == otherPos && it.side == side }
    }

    fun getCache(info: ChestRenderInfo): DrawCache =
            caches.getOrPut(info) { DrawCache() }

    fun getRenderInfo(side: Direction, rotation: Direction, surfaces: List<RenderSurface>, lightUv: Int):
            ChestRenderInfo {

        if (!isDoubleChest || !isOtherVisible(side, surfaces)) {
            return ChestRenderInfo.single(lightUv)
        }

        val secondChestDir = ChestBlock.getFacing(entity.cachedState)
        return if (side.axis != Direction.Axis.Y) {
            ChestRenderInfo(
                    drawInterleaved = side.axis != secondChestDir.axis,
                    drawSecond = side.rotateYClockwise() == secondChestDir,
                    hasSecond = side.axis != secondChestDir.axis,
                    lightUv = lightUv
            )
        } else {
            when (secondChestDir) {
                rotation.rotateYCounterclockwise() -> ChestRenderInfo(
                        drawInterleaved = true,
                        drawSecond = true,
                        lightUv = lightUv
                )
                rotation.rotateYClockwise() -> ChestRenderInfo(
                        drawInterleaved = true,
                        drawSecond = false,
                        lightUv = lightUv
                )
                else -> {
                    if (side == Direction.UP) {
                        ChestRenderInfo(
                                drawInterleaved = false,
                                drawSecond = rotation == secondChestDir,
                                lightUv = lightUv
                        )
                    } else {
                        // down
                        ChestRenderInfo(
                                drawInterleaved = false,
                                drawSecond = rotation.opposite == secondChestDir,
                                lightUv = lightUv
                        )
                    }
                }
            }
        }
    }

    private fun updatePrimaryRenderState() {
        stopContentRequest()
        clearCache()
        primaryRenderState = findPrimaryRenderState()
        if (primaryRenderState == this) {
            requestContents()
        }
    }

    private fun findPrimaryRenderState(): ChestRenderState {
        if (!isDoubleChest) {
            return this
        }

        val secondPos = otherBlock ?: return this

        val secondState = entity.world!!.getBlockState(secondPos)
        if (entity.cachedState.block != secondState.block) {
            // no match...
            return this
        }

        // try getting the other render state
        val otherState = OverlayRenderer.getRenderState(secondPos) as? ChestRenderState
                ?: return this

        return otherState.primaryRenderState
    }

    fun clearCache() {
        caches.values.forEach { it.destroy() }
    }

    override fun destroy() {
        // notify others
        val pos = otherBlock
        if (pos != null) {
            (OverlayRenderer.getRenderState(pos) as? ChestRenderState)?.updatePrimaryRenderState()
        }

        clearCache()

        super.destroy()
    }

    override fun onInventoryReceived(inventory: List<ItemStack>) {
        super.onInventoryReceived(inventory.subList(0, if (isDoubleChest) 54 else 27))
    }
}