package com.blasphemy.client.screen

import com.blasphemy.Blasphemy
import com.blasphemy.client.networking.ClientNetworking
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.awt.Color

/**
 * 对话界面
 */
class DialogueScreen(
    private val npcId: String,
    private val npcName: String,
    private val dialogueId: String,
    private val dialogueText: String,
    private val options: List<String>
) : Screen(Text.literal(npcName)) {
    
    // UI常量
    private val WIDTH = 320
    private val HEIGHT = 240
    private val BACKGROUND_COLOR = Color(20, 20, 50, 180).rgb
    private val BORDER_COLOR = Color(80, 80, 180, 255).rgb
    private val TITLE_COLOR = 0xFFD700 // 金色
    private val TEXT_COLOR = 0xFFFFFF // 白色
    private val OPTION_BG_COLOR = Color(40, 40, 70, 200).rgb
    
    // NPC肖像 - 默认使用Steve皮肤，可以根据NPC ID更改
    private val npcTexture = Identifier("textures/entity/steve.png")
    
    /**
     * 初始化界面
     */
    override fun init() {
        super.init()
        
        // 计算界面位置
        val startX = (width - WIDTH) / 2
        val startY = (height - HEIGHT) / 2
        
        // 添加对话选项按钮
        val buttonWidth = 180
        val buttonHeight = 20
        val buttonStartX = startX + (WIDTH - buttonWidth) / 2
        val buttonStartY = startY + HEIGHT - options.size * (buttonHeight + 5) - 15
        
        // 为每个选项创建按钮
        options.forEachIndexed { index, option ->
            val buttonY = buttonStartY + index * (buttonHeight + 5)
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal(option)) { button -> 
                    selectOption(index)
                }
                .position(buttonStartX, buttonY)
                .size(buttonWidth, buttonHeight)
                .build()
            )
        }
    }
    
    /**
     * 渲染界面
     */
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(context)
        
        // 计算界面位置
        val startX = (width - WIDTH) / 2
        val startY = (height - HEIGHT) / 2
        
        // 绘制对话框背景和边框
        context.fill(startX, startY, startX + WIDTH, startY + HEIGHT, BACKGROUND_COLOR)
        drawBorder(context, startX, startY, WIDTH, HEIGHT, 2, BORDER_COLOR)
        
        // 绘制NPC肖像区域
        val portraitSize = 64
        val portraitX = startX + 20
        val portraitY = startY + 40
        
        // 肖像背景和边框
        context.fill(portraitX, portraitY, portraitX + portraitSize, portraitY + portraitSize, OPTION_BG_COLOR)
        drawBorder(context, portraitX, portraitY, portraitSize, portraitSize, 1, BORDER_COLOR)
        
        // 渲染NPC肖像（默认使用Steve皮肤）
        context.drawTexture(npcTexture, portraitX, portraitY, 0f, 0f, portraitSize, portraitSize, portraitSize, portraitSize)
        
        // 绘制NPC名称
        val nameX = startX + (WIDTH - textRenderer.getWidth(title)) / 2
        context.drawTextWithShadow(textRenderer, title, nameX, startY + 15, TITLE_COLOR)
        
        // 绘制对话文本
        val dialogueX = portraitX + portraitSize + 20
        val dialogueY = portraitY
        val dialogueWidth = WIDTH - (dialogueX - startX) - 20
        
        // 创建文本包装
        val wrappedLines = textRenderer.wrapLines(Text.literal(dialogueText), dialogueWidth)
        
        // 绘制每行文本
        wrappedLines.forEachIndexed { index, line ->
            context.drawTextWithShadow(textRenderer, line, dialogueX, dialogueY + index * (textRenderer.fontHeight + 2), TEXT_COLOR)
        }
        
        // 绘制选项前的提示（如果有选项）
        if (options.isNotEmpty()) {
            val optionsHintText = Text.translatable("dialogue.options.hint")
            val hintX = startX + (WIDTH - textRenderer.getWidth(optionsHintText)) / 2
            val hintY = startY + HEIGHT - options.size * 25 - 30
            context.drawTextWithShadow(textRenderer, optionsHintText, hintX, hintY, TITLE_COLOR)
        }
        
        // 绘制按钮背景
        val buttonWidth = 180
        val buttonHeight = 20
        val buttonStartX = startX + (WIDTH - buttonWidth) / 2
        val buttonStartY = startY + HEIGHT - options.size * (buttonHeight + 5) - 15
        
        options.forEachIndexed { index, _ ->
            val buttonY = buttonStartY + index * (buttonHeight + 5)
            context.fill(buttonStartX - 2, buttonY - 2, buttonStartX + buttonWidth + 2, buttonY + buttonHeight + 2, BORDER_COLOR)
        }
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    /**
     * 绘制边框
     */
    private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, thickness: Int, color: Int) {
        // 上边框
        context.fill(x, y, x + width, y + thickness, color)
        // 下边框
        context.fill(x, y + height - thickness, x + width, y + height, color)
        // 左边框
        context.fill(x, y, x + thickness, y + height, color)
        // 右边框
        context.fill(x + width - thickness, y, x + width, y + height, color)
    }
    
    /**
     * 选择对话选项
     */
    private fun selectOption(index: Int) {
        Blasphemy.logger.info("选择对话选项：$index")
        
        // 根据npcId是否为空来确定使用哪个方法发送选项
        if (npcId.isEmpty() || dialogueId.isEmpty()) {
            // 使用旧方法发送选项到服务器
            ClientNetworking.sendDialogueOptionSelected(index)
        } else {
            // 使用新方法发送选项到服务器
            ClientNetworking.sendDialogueOption(npcId, dialogueId, index)
        }
        
        // 关闭界面
        this.close()
    }
    
    /**
     * 处理键盘输入
     */
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // ESC键关闭对话
        if (keyCode == 256) {
            this.close()
            return true
        }
        
        // 数字键1-9选择对话选项
        if (keyCode >= 49 && keyCode <= 57) { // 1-9键
            val optionIndex = keyCode - 49 // 0-8
            if (optionIndex < options.size) {
                selectOption(optionIndex)
                return true
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
} 