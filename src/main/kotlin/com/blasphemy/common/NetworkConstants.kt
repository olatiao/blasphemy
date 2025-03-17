package com.blasphemy.common

import net.minecraft.util.Identifier

/**
 * 网络常量
 */
object NetworkConstants {
    // 对话相关
    val DIALOGUE_DATA_PACKET_ID = Identifier("blasphemy", "dialogue_data")
    val DIALOGUE_OPTION_PACKET_ID = Identifier("blasphemy", "dialogue_option")
    
    // 任务相关
    val QUEST_DATA_PACKET_ID = Identifier("blasphemy", "quest_data")
    val QUEST_PROGRESS_PACKET_ID = Identifier("blasphemy", "quest_progress")
}