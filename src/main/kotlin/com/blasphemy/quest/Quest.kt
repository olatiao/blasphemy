package com.blasphemy.quest

import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

/**
 * 任务数据类
 */
data class Quest(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val objectiveKey: String,
    val type: String,        // 任务类型 (KILL, COLLECT, TALK, EXPLORE)
    val targetId: String,    // 目标ID (例如: minecraft:zombie, minecraft:diamond)
    val targetAmount: Int,   // 目标数量
    val rewards: List<QuestReward> = emptyList()
)

/**
 * 任务奖励数据类
 */
data class QuestReward(
    val type: String,        // 奖励类型 (ITEM, XP, MONEY)
    val itemId: String? = null,
    val amount: Int = 0,
    val nbt: String? = null
) {
    /**
     * 将奖励转换为物品堆
     */
    fun toItemStack(): ItemStack? {
        if (type != "ITEM" || itemId == null) return null
        
        val item = net.minecraft.registry.Registries.ITEM.get(Identifier.tryParse(itemId))
        return ItemStack(item, amount)
    }
}

/**
 * 任务进度数据类
 */
data class QuestProgress(
    val questId: String,
    var currentAmount: Int = 0,
    var completed: Boolean = false
) 