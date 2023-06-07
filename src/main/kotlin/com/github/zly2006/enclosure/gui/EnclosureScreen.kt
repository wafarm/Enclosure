package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket
import com.github.zly2006.enclosure.utils.formatSelection
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Consumer

class EnclosureScreen(handler: EnclosureScreenHandler, inventory: PlayerInventory?, title: Text?) :
    HandledScreen<EnclosureScreenHandler?>(handler, inventory, title) {
    val area: ReadOnlyEnclosureArea
    var permissionTargetListWidget: PermissionTargetListWidget? = null
    lateinit var globalWidget: ButtonWidget
    lateinit var playerWidget: ButtonWidget
    lateinit var unlistedWidget: ButtonWidget
    lateinit var aboutWidget: ButtonWidget
    val textWidgets: MutableList<ClickableTextWidget> = ArrayList()
    val subLandWidgets: MutableList<ClickableTextWidget> = ArrayList()
    var renderBottom = 5

    init {
        area = handler.area
    }

    override fun init() {
        super.init()
        textWidgets.clear()
        subLandWidgets.clear()
        permissionTargetListWidget = addDrawableChild(
            PermissionTargetListWidget(
                client,
                area,
                handler!!.fullName,
                this,
                width,
                height,
                60,
                height
            )
        )
        globalWidget =
            addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.global")) { button: ButtonWidget? ->
                assert(client != null)
                client!!.setScreen(PermissionScreen(area, UUID(0, 0), handler!!.fullName, this))
            }
                .size(100, 20)
                .position(5, 35)
                .build())
        playerWidget =
            addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.player")) { button: ButtonWidget ->
                assert(client != null)
                button.active = false
                unlistedWidget.active = true
                permissionTargetListWidget!!.showPlayers()
            }
                .size(100, 20)
                .position(110, 35)
                .build())
        unlistedWidget =
            addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.unspecified_player")) { button: ButtonWidget ->
                assert(client != null)
                button.active = false
                playerWidget.active = true
                permissionTargetListWidget!!.showUnlistedPlayers()
            }
                .size(100, 20)
                .position(215, 35)
                .build())
        aboutWidget =
            addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.about")) { button: ButtonWidget? ->
                assert(client != null)
                client!!.setScreen(AboutScreen(this))
            }
                .size(50, 20)
                .position(320, 35)
                .build())
        val owner = UUIDCacheS2CPacket.getName(area.owner)
        assert(client != null)
        if (handler!!.fatherFullName.isNotEmpty()) {
            textWidgets.add(ClickableTextWidget(
                client!!, this, Text.literal("<<< ")
                    .styled { style: Style -> style.withColor(Formatting.DARK_GREEN) }
                    .append(Text.literal(handler!!.fatherFullName).formatted(Formatting.GOLD)),
                Text.translatable("enclosure.widget.father_land.hover"),
                {
                    assert(client!!.player != null)
                    close()
                    client!!.player!!.networkHandler.sendChatCommand("enclosure gui " + handler!!.fatherFullName)
                }, 5, 5, width - 10
            )
            )
        }
        textWidgets.add(ClickableTextWidget(
            client!!, this, Text.empty()
                .append(Text.literal(area.fullName).styled { style: Style -> style.withColor(Formatting.GOLD) })
                .append(" ")
                .append(Text.translatable("enclosure.info.created_by"))
                .append(" ")
                .append(
                    if (owner == null) Text.translatable("enclosure.message.unknown_user").styled { style: Style ->
                        style.withColor(
                            Formatting.RED
                        )
                    } else Text.literal(owner).styled { style: Style -> style.withColor(Formatting.GOLD) })
                .append(", ")
                .append(Text.translatable("enclosure.info.created_on"))
                .append(Text.literal(SimpleDateFormat().format(area.createdOn)).styled { style: Style ->
                    style.withColor(
                        Formatting.GOLD
                    )
                }),
            null, null,
            5, 5, width - 10
        )
        )
        textWidgets.add(
            ClickableTextWidget(
                client!!, this,
                formatSelection(handler!!.worldId, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ),
                Text.translatable("enclosure.widget.selection_render.hover"),
                {
                    assert(client!!.player != null)
                    client!!.player!!.networkHandler.sendChatCommand("enclosure select land " + handler!!.fullName)
                    close()
                }, 5, 20, width - 10
            )
        )
        for (name in handler!!.subAreaNames) {
            subLandWidgets.add(ClickableTextWidget(
                client!!, this, Text.literal(">>> ")
                    .styled { style: Style -> style.withColor(Formatting.DARK_GREEN) }
                    .append(Text.literal(name).formatted(Formatting.GOLD)),
                Text.translatable("enclosure.widget.sub_land.hover"),
                {
                    assert(client!!.player != null)
                    close()
                    client!!.player!!.networkHandler.sendChatCommand(
                        "enclosure gui %s.%s".formatted(
                            handler!!.fullName,
                            name
                        )
                    )
                }, 5, 5, 0
            )
            )
        }
    }

    override fun render(drawContext: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(drawContext)
        renderBottom = 5
        for (textWidget in textWidgets) {
            textWidget.y = renderBottom
            renderBottom += textWidget.calcHeight()
        }
        for (textWidget in subLandWidgets) {
            textWidget.y = renderBottom
        }
        renderBottom += if (subLandWidgets.isEmpty()) 0 else 10
        globalWidget.y = renderBottom
        playerWidget.y = renderBottom
        unlistedWidget.y = renderBottom
        aboutWidget.y = renderBottom
        permissionTargetListWidget!!.top = renderBottom + 25
        super.render(drawContext, mouseX, mouseY, delta)
        for (textWidget in textWidgets) {
            textWidget.render(drawContext, mouseX, mouseY, delta)
        }
        var subLandsX = 5
        for (textWidget in subLandWidgets) {
            textWidget.x = subLandsX
            textWidget.render(drawContext, mouseX, mouseY, delta)
            subLandsX += textWidget.width + 5
        }
    }

    override fun children(): List<Element> {
        val children: MutableList<Element> = ArrayList(super.children())
        children.addAll(textWidgets)
        children.addAll(subLandWidgets)
        return children
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        textWidgets.forEach(Consumer { textWidget: ClickableTextWidget -> textWidget.width = width })
        super.resize(client, width, height)
    }

    override fun drawBackground(context: DrawContext?, delta: Float, mouseX: Int, mouseY: Int) { }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            close()
            return true
        }
        return if (focused == null) {
            false
        } else focused!!.keyPressed(keyCode, scanCode, modifiers)
    }

    fun requestConfirm(message: Text?) {
        assert(client != null)
        client!!.execute {
            client!!.setScreen(ConfirmScreen(this, message!!) {
                assert(client!!.player != null)
                client!!.player!!.networkHandler.sendCommand("enclosure confirm")
            })
        }
    }
}
