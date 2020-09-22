package com.xxmicloxx.blockoverlay.render

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.Sprite
import net.minecraft.client.texture.SpriteAtlasHolder
import net.minecraft.client.texture.TextureManager
import net.minecraft.resource.ReloadableResourceManager
import net.minecraft.util.Identifier
import java.util.stream.Stream

data class Texture(val id: Identifier) {
    constructor(id: String) : this(Identifier(id))
}

object Textures {
    val BACKGROUND = Texture("blockoverlay:background")
    val BACKGROUND_DOUBLE = Texture("blockoverlay:background_double")
    val LOADER = Texture("blockoverlay:loader")
    val ERROR = Texture("blockoverlay:error")
    val ITEM_FRAME = Texture("blockoverlay:item_frame")
    val ITEM_BAR_RED = Texture("blockoverlay:item_bar_red")
    val ITEM_BAR_YELLOW = Texture("blockoverlay:item_bar_yellow")
    val ITEM_BAR_GREEN = Texture("blockoverlay:item_bar_green")
    val ITEM_BAR_BLUE = Texture("blockoverlay:item_bar_blue")
    val ITEM_BAR_PURPLE = Texture("blockoverlay:item_bar_purple")
    val FLAMES = Texture("blockoverlay:flames")

    val ATLAS_ID = Identifier("blockoverlay:textures/atlas/overlay.png")

    private val ALL = listOf(BACKGROUND, LOADER, ERROR, ITEM_FRAME, ITEM_BAR_RED, ITEM_BAR_YELLOW, ITEM_BAR_GREEN,
            ITEM_BAR_BLUE, ITEM_BAR_PURPLE, FLAMES, BACKGROUND_DOUBLE)

    lateinit var atlas: Atlas
        private set

    fun renderInit() {
        atlas = Atlas(MC.textureManager)
        (MinecraftClient.getInstance().resourceManager as ReloadableResourceManager).registerListener(atlas)
    }

    class Atlas internal constructor(textureManager: TextureManager) :
            SpriteAtlasHolder(textureManager, ATLAS_ID, "overlay") {

        override fun getSprites(): Stream<Identifier> {
            return ALL.stream().map { it.id }
        }

        fun getSprite(texture: Texture): Sprite {
            return getSprite(texture.id)
        }
    }
}

object BarTextureRegistry {
    private val textureRegistry = listOf(
            Entry(Textures.ITEM_BAR_RED, { 0 }, { ss -> ss }),
            Entry(Textures.ITEM_BAR_YELLOW, { ss -> ss }, { ss -> 3 * ss }),
            Entry(Textures.ITEM_BAR_GREEN, { ss -> 3 * ss }, { ss -> 9 * ss }),
            Entry(Textures.ITEM_BAR_BLUE, { ss -> 9 * ss }, { ss -> 27 * ss }),
            Entry(Textures.ITEM_BAR_PURPLE, { ss -> 27 * ss }, { ss -> 54 * ss })
    )

    data class Entry(val texture: Texture, val startCount: (Int) -> Int, val endCount: (Int) -> Int) {
        fun getPercentage(count: Int, stackSize: Int): Float {
            return count.toFloat() / endCount(stackSize).toFloat()
        }
    }

    fun findEntry(count: Int, stackSize: Int): Entry {
        var highest: Entry? = null
        var highestCount = 0
        var lowest: Entry? = null
        var lowestCount = 0

        for (entry in textureRegistry) {
            val min = entry.startCount(stackSize)
            val max = entry.endCount(stackSize)
            if (count in (min + 1)..max) {
                // found it
                return entry
            }

            if (lowest == null || min < lowestCount) {
                lowest = entry
                lowestCount = min
            }
            if (highest == null || max > highestCount) {
                highest = entry
                highestCount = max
            }
        }

        return when {
            count > highestCount -> highest!!
            count < lowestCount -> lowest!!
            else -> {
                // undefined area
                throw RuntimeException("Undefined bar area")
            }
        }
    }
}