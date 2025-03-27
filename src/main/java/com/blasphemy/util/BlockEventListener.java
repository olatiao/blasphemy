package com.blasphemy.util;

import com.blasphemy.Blasphemy;
import com.blasphemy.portal.PortalFrameValidator;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 监听方块事件，用于处理与方块相关的游戏逻辑
 */
public class BlockEventListener {
    /**
     * 初始化事件监听器
     */
    public static void init() {
        Blasphemy.LOGGER.info("注册方块事件监听器...");
        registerBlockBreakEvent();
    }

    /**
     * 注册方块破坏事件
     */
    private static void registerBlockBreakEvent() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // 如果破坏的是传送门框架方块，清除周围的传送门方块
            if (PortalFrameValidator.isValidFrameBlock(world, pos)) {
                Blasphemy.LOGGER.info("玩家 {} 破坏了传送门框架方块 {} 在 {}", 
                    player.getName().getString(), state.getBlock().getName().getString(), pos);
                cleanupPortalBlocks(world, pos);
            }
            
            // 如果破坏的是传送门方块，清除缓存
            if (state.getBlock() == Blocks.NETHER_PORTAL) {
                Blasphemy.LOGGER.info("玩家 {} 破坏了传送门方块在 {}", 
                    player.getName().getString(), pos);
                cleanupPortalBlocks(world, pos);
            }
        });
    }
    
    /**
     * 清理周围的传送门方块
     */
    private static void cleanupPortalBlocks(World world, BlockPos pos) {
        Blasphemy.LOGGER.debug("开始清理传送门方块，中心位置：{}", pos);
        int count = 0;
        
        // 清除周围的传送门方块
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    BlockState checkState = world.getBlockState(checkPos);
                    if (checkState.getBlock() == Blocks.NETHER_PORTAL) {
                        world.setBlockState(checkPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        count++;
                    }
                }
            }
        }
        
        if (count > 0) {
            Blasphemy.LOGGER.info("清理了 {} 个传送门方块", count);
        }
    }
} 