package com.blasphemy.client.gui

import com.blasphemy.Blasphemy
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 对话界面，用于显示NPC对话和选项
 */
class DialogueScreen(
    private val npcName: String,
    private val dialogueText: String,
    private val options: List<String>
) : Screen(Text.literal("Dialogue")) {
    
    // 对话框背景纹理
    private val DIALOGUE_BACKGROUND = Identifier(Blasphemy.MOD_ID, "textures/gui/dialogue_background.png")
    
    // 背景宽度和高度
    private val BACKGROUND_WIDTH = 320
    private val BACKGROUND_HEIGHT = 180
    
    // 选项按钮列表
    private val optionButtons = mutableListOf<ButtonWidget>()
    
    // 当选择选项时的回调函数
    private var onOptionSelected: ((Int) -> Unit)? = null
    
    override fun init() {
        super.init()
        
        // 计算对话框左上角坐标
        val x = (width - BACKGROUND_WIDTH) / 2
        val y = (height - BACKGROUND_HEIGHT) / 2
        
        // 创建选项按钮
        options.forEachIndexed { index, option ->
            val buttonWidth = 280
            val buttonHeight = 20
            val buttonX = x + (BACKGROUND_WIDTH - buttonWidth) / 2
            val buttonY = y + 100 + index * (buttonHeight + 5)
            
            val button = ButtonWidget.builder(Text.literal(option)) { button ->
                // 当按钮被点击时
                onOptionSelected?.invoke(index)
                close()
            }
            .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
            .build()
            
            optionButtons.add(button)
            addDrawableChild(button)
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 渲染半透明背景
        renderBackground(context)
        
        // 计算对话框左上角坐标
        val x = (width - BACKGROUND_WIDTH) / 2
        val y = (height - BACKGROUND_HEIGHT) / 2
        
        // 渲染对话框背景
        context.drawTexture(DIALOGUE_BACKGROUND, x, y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT)
        
        // 渲染NPC名称
        val npcNameText = Text.literal(npcName)
        context.drawTextWithShadow(
            textRenderer,
            npcNameText,
            x + 20,
            y + 20,
            0xFFFFFF
        )
        
        // 渲染对话文本
        val wrappedText = textRenderer.wrapLines(Text.literal(dialogueText), 280)
        wrappedText.forEachIndexed { index, text ->
            context.drawTextWithShadow(
                textRenderer,
                text,
                x + 20,
                y + 40 + index * 10,
                0xFFFFFF
            )
        }
        
        // 渲染按钮
        super.render(context, mouseX, mouseY, delta)
    }
    
    override fun shouldPause(): Boolean {
        // 对话界面打开时暂停游戏
        return true
    }
    
    /**
     * 设置选项选择回调
     */
    fun setOptionSelectedCallback(callback: (Int) -> Unit) {
        this.onOptionSelected = callback
    }
    
    companion object {
        /**
         * 打开对话界面
         */
        fun open(npcName: String, dialogueText: String, options: List<String>, callback: (Int) -> Unit) {
            val client = MinecraftClient.getInstance()
            val screen = DialogueScreen(npcName, dialogueText, options)
            screen.setOptionSelectedCallback(callback)
            client.setScreen(screen)
        }
    }
} 