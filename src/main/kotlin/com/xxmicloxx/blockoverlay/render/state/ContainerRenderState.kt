package com.xxmicloxx.blockoverlay.render.state

import com.xxmicloxx.blockoverlay.ContainerHelper
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack

abstract class ContainerRenderState<out T : BlockEntity>(entity: T) : BlockRenderState<T>(entity),
        ContainerHelper.ContentReceiver {

    enum class Status {
        LOADING, FAILED, LOADED
    }

    var status = Status.LOADING
    var inventory: List<ItemStack> = listOf()
    private var needsUpdate = true

    fun pollUpdate(): Boolean {
        val update = needsUpdate
        needsUpdate = false
        return update
    }

    protected fun requestContents() {
        ContainerHelper.requestContainerContents(entity, this)
    }

    protected fun stopContentRequest() {
        ContainerHelper.cancelContentRequest(entity)
    }

    override fun destroy() {
        stopContentRequest()
        super.destroy()
    }

    override fun onInventoryReceived(inventory: List<ItemStack>) {
        status = Status.LOADED
        this.inventory = inventory
        needsUpdate = true
    }

    override fun onPropertyReceived(id: Int, value: Int) {
    }

    override fun onInventoryReceiveError() {
        status = Status.FAILED
    }
}