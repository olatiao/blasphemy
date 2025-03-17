package com.blasphemy.client.screen

import com.blasphemy.client.ui.QuestEntry
import com.blasphemy.client.ui.QuestTracker
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import java.awt.Color

/**
 * 任务中心界面，显示所有已接取的任务详情
 */
class QuestCenterScreen : Screen(Text.translatable("quest.center.title")) {
    
    // UI常量
    private val BACKGROUND_COLOR = Color(20, 20, 50, 220).rgb
    private val BORDER_COLOR = Color(80, 80, 180, 255).rgb
    private val TITLE_COLOR = 0xFFD700 // 金色
    private val TEXT_COLOR = 0xFFFFFF // 白色
    private val SECTION_BG_COLOR = Color(40, 40, 70, 200).rgb
    private val PROGRESS_BAR_BG = Color(60, 60, 60, 180).rgb
    private val PROGRESS_BAR_FILL = Color(80, 180, 80, 220).rgb
    private val COMPLETED_COLOR = Color(120, 255, 120, 255).rgb
    
    // 界面尺寸
    private val WIDTH = 320
    private val HEIGHT = 240
    
    // 任务列表滚动位置
    private var scrollOffset = 0
    private val MAX_DISPLAYED_QUESTS = 5
    private val QUEST_ENTRY_HEIGHT = 60
    
    init {
        // 初始化时加载任务数据
        loadQuestData()
    }
    
    override fun init() {
        super.init()
        
        // 计算界面边界
        val startX = (width - WIDTH) / 2
        val startY = (height - HEIGHT) / 2
        
        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.close")) { button ->
            this.close()
        }.position(startX + WIDTH / 2 - 40, startY + HEIGHT - 30).size(80, 20).build())
        
        // 添加滚动按钮
        if (QuestTracker.getInstance().getActiveQuests().size > MAX_DISPLAYED_QUESTS) {
            // 上滚按钮
            this.addDrawableChild(ButtonWidget.builder(Text.of("↑")) { button ->
                if (scrollOffset > 0) scrollOffset--
            }.position(startX + WIDTH - 25, startY + 50).size(20, 20).build())
            
            // 下滚按钮
            this.addDrawableChild(ButtonWidget.builder(Text.of("↓")) { button ->
                if (scrollOffset < QuestTracker.getInstance().getActiveQuests().size - MAX_DISPLAYED_QUESTS) scrollOffset++
            }.position(startX + WIDTH - 25, startY + HEIGHT - 70).size(20, 20).build())
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(context)
        
        // 计算界面边界
        val startX = (width - WIDTH) / 2
        val startY = (height - HEIGHT) / 2
        
        // 绘制主面板背景
        context.fill(startX, startY, startX + WIDTH, startY + HEIGHT, BACKGROUND_COLOR)
        drawBorder(context, startX, startY, WIDTH, HEIGHT, 2, BORDER_COLOR)
        
        // 绘制标题
        val titleText = this.title
        val titleX = startX + (WIDTH - textRenderer.getWidth(titleText)) / 2
        context.drawTextWithShadow(textRenderer, titleText, titleX, startY + 10, TITLE_COLOR)
        
        // 绘制任务列表
        val questListX = startX + 10
        val questListY = startY + 40
        val questListWidth = WIDTH - 20
        val questListHeight = HEIGHT - 80
        
        // 绘制任务列表背景
        context.fill(questListX, questListY, questListX + questListWidth, questListY + questListHeight, SECTION_BG_COLOR)
        drawBorder(context, questListX, questListY, questListWidth, questListHeight, 1, BORDER_COLOR)
        
        // 绘制任务条目
        val quests = QuestTracker.getInstance().getActiveQuests()
        if (quests.isEmpty()) {
            // 如果没有任务，显示提示信息
            val noQuestText = Text.translatable("quest.center.no_quests")
            val noQuestX = questListX + (questListWidth - textRenderer.getWidth(noQuestText)) / 2
            val noQuestY = questListY + (questListHeight - textRenderer.fontHeight) / 2
            context.drawTextWithShadow(textRenderer, noQuestText, noQuestX, noQuestY, TEXT_COLOR)
        } else {
            // 显示可见的任务
            val visibleQuests = quests.drop(scrollOffset).take(MAX_DISPLAYED_QUESTS)
            visibleQuests.forEachIndexed { index, quest ->
                val questY = questListY + index * QUEST_ENTRY_HEIGHT
                renderQuestDetailEntry(context, questListX, questY, questListWidth, quest)
            }
        }
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    /**
     * 渲染单个任务详情条目
     */
    private fun renderQuestDetailEntry(context: DrawContext, x: Int, y: Int, width: Int, quest: QuestEntry) {
        // 绘制任务条目背景
        val entryBgColor = if (quest.completed) Color(40, 80, 40, 200).rgb else SECTION_BG_COLOR
        context.fill(x, y, x + width, y + QUEST_ENTRY_HEIGHT - 5, entryBgColor)
        drawBorder(context, x, y, width, QUEST_ENTRY_HEIGHT - 5, 1, BORDER_COLOR)
        
        // 绘制任务标题
        val titleText = Text.translatable(quest.nameKey)
        context.drawTextWithShadow(textRenderer, titleText, x + 5, y + 5, TITLE_COLOR)
        
        // 绘制接取自NPC信息
        if (quest.npcNameKey.isNotEmpty()) {
            val npcText = Text.translatable("quest.center.from_npc", Text.translatable(quest.npcNameKey))
            context.drawTextWithShadow(textRenderer, npcText, x + 5, y + 17, TEXT_COLOR)
        }
        
        // 绘制任务目标
        val objectiveText = Text.translatable(quest.objectiveKey)
        context.drawTextWithShadow(textRenderer, objectiveText, x + 5, y + 29, TEXT_COLOR)
        
        // 绘制任务状态
        val statusText = if (quest.completed) {
            Text.translatable("quest.center.status.completed")
        } else {
            Text.translatable("quest.center.status.in_progress")
        }
        context.drawTextWithShadow(textRenderer, statusText, x + width - 5 - textRenderer.getWidth(statusText), y + 5, 
            if (quest.completed) COMPLETED_COLOR else TEXT_COLOR)
        
        // 绘制进度条
        val progressBarWidth = width - 10
        val progressBarHeight = 5
        val progressBarX = x + 5
        val progressBarY = y + QUEST_ENTRY_HEIGHT - 15
        
        // 进度条背景
        context.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, PROGRESS_BAR_BG)
        
        // 进度条填充
        val fillWidth = (progressBarWidth * quest.progress / quest.maxProgress).toInt()
        if (fillWidth > 0) {
            val fillColor = if (quest.completed) COMPLETED_COLOR else PROGRESS_BAR_FILL
            context.fill(progressBarX, progressBarY, progressBarX + fillWidth, progressBarY + progressBarHeight, fillColor)
        }
        
        // 进度文本
        val progressText = "${quest.progress}/${quest.maxProgress}"
        val progressX = progressBarX + (progressBarWidth - textRenderer.getWidth(progressText)) / 2
        context.drawTextWithShadow(textRenderer, progressText, progressX, progressBarY - 10, TEXT_COLOR)
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
     * 加载任务数据
     */
    private fun loadQuestData() {
        // 任务数据已通过QuestTracker加载
    }
    
    /**
     * 处理键盘输入
     */
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // ESC键关闭界面
        if (keyCode == 256) {
            this.close()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    /**
     * 关闭界面
     */
    override fun close() {
        client?.setScreen(null)
    }
} 