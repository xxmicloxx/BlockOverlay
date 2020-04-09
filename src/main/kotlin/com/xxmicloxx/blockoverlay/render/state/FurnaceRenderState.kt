package com.xxmicloxx.blockoverlay.render.state

import com.xxmicloxx.blockoverlay.ContainerHelper
import net.minecraft.block.entity.FurnaceBlockEntity
import net.minecraft.item.ItemStack

class FurnaceRenderState(entity: FurnaceBlockEntity) :
    BlockRenderState<FurnaceBlockEntity>(entity), ContainerHelper.ContentReceiver {

    enum class Status {
        LOADING, FAILED, DONE
    }

    var status: Status = Status.LOADING
    var inventory: List<ItemStack>? = null

    init {
        fetchData()
    }

    private fun fetchData() {
        ContainerHelper.requestContainerContents(entity, this)
    }

    override fun destroy() {
        ContainerHelper.cancelContentRequest(entity)
        super.destroy()
    }

    override fun onInventoryReceived(inventory: List<ItemStack>) {
        status = Status.DONE
        this.inventory = inventory
    }

    override fun onPropertyReceived(id: Int, value: Int) {
    }

    override fun onInventoryReceiveError() {
        status = Status.FAILED
    }
}