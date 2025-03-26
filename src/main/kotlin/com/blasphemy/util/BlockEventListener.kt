package com.blasphemy.util

import com.blasphemy.Blasphemy
import com.blasphemy.portal.PortalFrameValidator
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * 监听方块事件，用于处理与方块相关的游戏逻辑
 */
class BlockEventListener {
    companion object {
        /**
         * 初始化方块事件监听器
         */
        @JvmStatic
        fun init() {
            // 注册方块破坏事件监听器
            registerBlockBreakEvent()
            Blasphemy.logger.info("方块事件监听器初始化完成")
        }

        /**
         * 注册方块破坏事件监听
         */
        private fun registerBlockBreakEvent() {
            PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, _ ->
                // 获取方块ID
                val blockId = Registries.BLOCK.getId(state.block).toString()
                
                // 检查被破坏的方块是否是传送门框架方块
                if (PortalFrameValidator.getValidFrameBlocks().contains(blockId)) {
                    Blasphemy.logger.info("检测到传送门框架方块被破坏: $pos")
                    // 通知传送门验证器，有方块被破坏，可能需要失效某些缓存
                    PortalFrameValidator.onBlockBreak(pos)
                }
                
                // 检查被破坏的方块是否是下界传送门方块
                if (state.block == Block.getBlockFromItem(Registries.ITEM.get(net.minecraft.util.Identifier("minecraft:nether_portal")))) {
                    // 当玩家破坏传送门方块时，清除附近可能的缓存结果
                    Blasphemy.logger.info("检测到传送门方块被破坏: $pos")
                    cleanupPortalCache(world, pos)
                }
            }
        }
        
        /**
         * 清理传送门缓存
         */
        private fun cleanupPortalCache(world: World, pos: BlockPos) {
            // 直接清除所有缓存，简单粗暴但有效
            // 对于更复杂的实现，可以只清除与该位置相关的缓存
            PortalFrameValidator.clearCache()
        }
    }
} 