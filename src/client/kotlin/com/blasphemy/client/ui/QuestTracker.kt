package com.blasphemy.client.ui

import com.blasphemy.Blasphemy
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 任务追踪器，在屏幕右侧显示当前活动任务
 */
class QuestTracker {
    companion object {
        // 单例实例
        private val instance = QuestTracker()
        fun getInstance(): QuestTracker = instance
        
        // UI常量
        private const val MAX_DISPLAYED_QUESTS = 5 // 最多显示5个任务
        private const val QUEST_PANEL_WIDTH = 160 // 任务面板宽度
        private const val QUEST_ENTRY_HEIGHT = 40 // 每个任务条目高度
        private const val RIGHT_MARGIN = 10 // 右侧边距
        
        // 颜色
        private val PANEL_BACKGROUND = Color(20, 20, 50, 180).rgb
        private val PANEL_BORDER = Color(80, 80, 180, 255).rgb
        private val TITLE_COLOR = 0xFFD700 // 金色
        private val TEXT_COLOR = 0xFFFFFF // 白色
        private val PROGRESS_BAR_BG = Color(60, 60, 60, 180).rgb
        private val PROGRESS_BAR_FILL = Color(80, 180, 80, 220).rgb
    }
    
    // 活动任务队列
    private val activeQuests = ConcurrentLinkedQueue<QuestEntry>()
    
    /**
     * 渲染任务追踪器
     */
    fun render(context: DrawContext) {
        if (activeQuests.isEmpty()) return
        
        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer
        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight
        
        // 计算起始Y坐标（居中显示）
        val totalHeight = minOf(activeQuests.size, MAX_DISPLAYED_QUESTS) * QUEST_ENTRY_HEIGHT
        val startY = (screenHeight - totalHeight) / 2
        
        // 绘制任务追踪器标题
        val titleText = Text.translatable("quest.tracker.title")
        val titleX = screenWidth - QUEST_PANEL_WIDTH - RIGHT_MARGIN + (QUEST_PANEL_WIDTH - textRenderer.getWidth(titleText)) / 2
        context.drawTextWithShadow(textRenderer, titleText, titleX, startY - 15, TITLE_COLOR)
        
        // 绘制每个任务
        activeQuests.take(MAX_DISPLAYED_QUESTS).forEachIndexed { index, quest ->
            val questY = startY + index * QUEST_ENTRY_HEIGHT
            renderQuestEntry(context, screenWidth - QUEST_PANEL_WIDTH - RIGHT_MARGIN, questY, quest, textRenderer)
        }
    }
    
    /**
     * 渲染单个任务条目
     */
    private fun renderQuestEntry(context: DrawContext, x: Int, y: Int, quest: QuestEntry, textRenderer: TextRenderer) {
        // 绘制背景和边框
        context.fill(x, y, x + QUEST_PANEL_WIDTH, y + QUEST_ENTRY_HEIGHT, PANEL_BACKGROUND)
        drawBorder(context, x, y, QUEST_PANEL_WIDTH, QUEST_ENTRY_HEIGHT, 1, PANEL_BORDER)
        
        // 绘制任务标题
        val titleText = Text.translatable(quest.nameKey)
        context.drawTextWithShadow(textRenderer, titleText, x + 5, y + 5, TITLE_COLOR)
        
        // 绘制任务目标
        val objectiveText = Text.translatable(quest.objectiveKey)
        context.drawTextWithShadow(textRenderer, objectiveText, x + 5, y + 17, TEXT_COLOR)
        
        // 绘制进度条
        val progressBarWidth = QUEST_PANEL_WIDTH - 10
        val progressBarHeight = 5
        val progressBarX = x + 5
        val progressBarY = y + QUEST_ENTRY_HEIGHT - 10
        
        // 背景
        context.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, PROGRESS_BAR_BG)
        
        // 填充部分
        val fillWidth = (progressBarWidth * quest.progress / quest.maxProgress).toInt()
        if (fillWidth > 0) {
            context.fill(progressBarX, progressBarY, progressBarX + fillWidth, progressBarY + progressBarHeight, PROGRESS_BAR_FILL)
        }
        
        // 绘制进度文本
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
     * 添加任务
     */
    fun addQuest(questId: String, nameKey: String, objectiveKey: String, maxProgress: Int) {
        // 如果任务已存在，则更新它
        val existingQuest = activeQuests.find { it.id == questId }
        if (existingQuest != null) {
            existingQuest.nameKey = nameKey
            existingQuest.objectiveKey = objectiveKey
            existingQuest.maxProgress = maxProgress
            existingQuest.progress = 0
        } else {
            // 添加新任务
            val quest = QuestEntry(questId, nameKey, objectiveKey, maxProgress)
            activeQuests.add(quest)
            Blasphemy.logger.info("添加任务到追踪器: $nameKey")
        }
    }
    
    /**
     * 更新任务进度
     */
    fun updateQuestProgress(questId: String, progress: Int) {
        val quest = activeQuests.find { it.id == questId }
        if (quest != null) {
            quest.progress = progress
            
            // 如果任务完成，标记为已完成
            if (progress >= quest.maxProgress) {
                quest.completed = true
                Blasphemy.logger.info("任务完成: ${quest.nameKey}")
            }
        }
    }
    
    /**
     * 移除任务
     */
    fun removeQuest(questId: String) {
        activeQuests.removeIf { it.id == questId }
    }
    
    /**
     * 清除所有任务
     */
    fun clearQuests() {
        activeQuests.clear()
    }
    
    /**
     * 获取活动任务列表
     */
    fun getActiveQuests(): List<QuestEntry> {
        return activeQuests.toList()
    }
}

/**
 * 任务条目数据类
 */
data class QuestEntry(
    val id: String,
    var nameKey: String,
    var objectiveKey: String,
    var maxProgress: Int,
    var progress: Int = 0,
    var completed: Boolean = false,
    var npcNameKey: String = ""
) 