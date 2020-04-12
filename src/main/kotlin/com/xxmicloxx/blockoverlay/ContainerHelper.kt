package com.xxmicloxx.blockoverlay

import com.xxmicloxx.blockoverlay.render.MC
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.GuiCloseC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Direction
import java.util.*

object ContainerHelper {
    data class ContentRequest(
        val entity: BlockEntity,
        val receiver: ContentReceiver,
        var syncId: Int? = null,
        var cancelled: Boolean = false,
        var remainingTicks: Int = TIMEOUT_TICKS)

    interface ContentReceiver {
        fun onInventoryReceived(inventory: List<ItemStack>)
        fun onPropertyReceived(id: Int, value: Int)
        fun onInventoryReceiveError()
    }

    private const val TIMEOUT_TICKS = 20
    private const val FAILURE_RETRY_TICKS = 20
    private const val RELOAD_TICKS = 100
    private const val REQUEST_DELAY_TICKS = 0
    private const val INTERACT_PAUSE_TICKS = 5

    private const val CLICK_DISTANCE = 5.0
    private const val CLICK_DISTANCE_SQUARED = CLICK_DISTANCE * CLICK_DISTANCE

    private var requestDelay = 0
    private var paused = false

    private val pendingRequests = ArrayDeque<ContentRequest>()
    private val requests = mutableListOf<ContentRequest>()
    var currentRequest: ContentRequest? = null
        private set

    fun requestContainerContents(entity: BlockEntity, receiver: ContentReceiver) {
        pendingRequests.push(ContentRequest(entity, receiver))
    }

    fun cancelContentRequest(entity: BlockEntity) {
        if (currentRequest?.entity == entity) {
            // cancel current request
            currentRequest?.cancelled = true
        }

        pendingRequests.removeIf { it.entity == entity }
        requests.removeIf { it.entity == entity }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun pauseForInteract() {
        requestDelay = INTERACT_PAUSE_TICKS
    }

    fun tick() {
        if (requestDelay > 0) {
            requestDelay--
        }

        checkProperties()
        requestNext()

        val request = currentRequest ?: return
        if (request.remainingTicks-- == 0) {
            // timeout
            if (!request.cancelled) {
                request.receiver.onInventoryReceiveError()
                request.remainingTicks = FAILURE_RETRY_TICKS
                requests.add(request)
            }

            currentRequest = null
            requestNext()
        }
    }

    private fun checkProperties() {
        requests.forEach { it.remainingTicks-- }

        requests
            .filter { it.remainingTicks == 0 }
            .forEach {
                requests.remove(it)
                it.remainingTicks = TIMEOUT_TICKS
                pendingRequests.add(it)
            }
    }

    private fun requestNext() {
        val player = MC.player
        if (player == null) {
            // not ingame
            // screw this
            clearRequests()
            return
        }

        if (paused) {
            return
        }

        // check if in GUI
        if (MC.currentScreen != null) {
            // this will close GUIs, retry later
            return
        }

        val skipped = mutableListOf<ContentRequest>()

        if (currentRequest == null && requestDelay == 0) {
            while (pendingRequests.isNotEmpty() && currentRequest == null) {
                val req = pendingRequests.remove()
                val pos = req.entity.pos

                if (player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) > CLICK_DISTANCE_SQUARED) {
                    // skip it
                    skipped.add(req)
                } else {
                    currentRequest = req
                }
            }

            pendingRequests.addAll(skipped)

            getContainerContents()
        }
    }

    private fun clearRequests() {
        currentRequest = null
        requestDelay = 0
        pendingRequests.clear()
        requests.clear()
    }

    private fun getContainerContents() {
        val player = MC.player ?: return
        val entity = currentRequest?.entity ?: return

        if (entity.world != MC.currentWorld) {
            // not in correct world...
            currentRequest = null
            requestNext()
            return
        }

        val sneaking = player.isSneaking

        if (sneaking) {
            // unsneak
            val packet = ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY)
            ClientSidePacketRegistry.INSTANCE.sendToServer(packet)
        }
        // send interact
        val blockHit = BlockHitResult(player.pos, Direction.NORTH, entity.pos, false)
        val openPacket = PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHit)
        ClientSidePacketRegistry.INSTANCE.sendToServer(openPacket)

        if (sneaking) {
            // sneak again
            val packet = ClientCommandC2SPacket(MC.player ?: return, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY)
            ClientSidePacketRegistry.INSTANCE.sendToServer(packet)
        }
    }

    fun gotContainerContents(inventory: List<ItemStack>) {
        val req = currentRequest ?: return

        if (!req.cancelled) {
            req.receiver.onInventoryReceived(inventory)
        }

        val packet = GuiCloseC2SPacket(req.syncId!!)
        ClientSidePacketRegistry.INSTANCE.sendToServer(packet)

        if (!req.cancelled) {
            req.remainingTicks = RELOAD_TICKS
            requests.add(req)
        }

        currentRequest = null

        if (requestDelay == 0) {
            requestDelay = REQUEST_DELAY_TICKS
        }

        if (requestDelay == 0) {
            requestNext()
        }
    }

    fun gotContainerProperties(guiId: Int, id: Int, value: Int): Boolean {
        var request = currentRequest
        if (request?.syncId != guiId) {
            request = requests.find { it.syncId == guiId }
        }

        if (request == null) {
            request = pendingRequests.find { it.syncId == guiId }
        }

        request ?: return false

        request.receiver.onPropertyReceived(id, value)
        return true
    }
}