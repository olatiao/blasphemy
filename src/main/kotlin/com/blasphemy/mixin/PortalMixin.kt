package com.blasphemy.mixin

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.NetherPortalBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.FlintAndSteelItem
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * 传送门创建Mixin，用于控制传送门的创建逻辑
 */
@Mixin(FlintAndSteelItem::class)
class PortalMixin {
    
    @Inject(
        method = ["useOnBlock"],
        at = [At("HEAD")],
        cancellable = true
    )
    private fun useOnBlock(
        context: ItemUsageContext,
        cir: CallbackInfoReturnable<ActionResult>
    ) {
        val player = context.player ?: return
        val world = context.world
        
        // 客户端不处理
        if (world.isClient) {
            return
        }
        
        val pos = context.blockPos
        val side = context.side
        val hitPos = context.blockPos.offset(side)
        
        // 获取配置
        val config = ModConfig.getConfig().portalConfig
        
        // 如果未启用自定义传送门，直接返回
        if (!config.enabled) {
            return
        }
        
        Blasphemy.Companion.logger.info("自定义传送门: 使用打火石在位置 ${pos.x}, ${pos.y}, ${pos.z}")
        
        // 检查点击的是不是允许的传送门框架方块
        val targetBlock = world.getBlockState(pos).block
        val targetBlockId = Registries.BLOCK.getId(targetBlock).toString()
        val portalBlocks = config.portalBlocks
        if (!portalBlocks.contains(targetBlockId)) {
            Blasphemy.Companion.logger.info("自定义传送门: 点击的不是允许的框架方块: $targetBlockId")
            return
        }
        
        Blasphemy.Companion.logger.info("自定义传送门: 点击了可能的传送门框架方块")
        
        // 直接使用硬编码的标准传送门尺寸进行检测
        val result = checkStandardPortal(world, pos)
        if (result == null) {
            Blasphemy.Companion.logger.info("自定义传送门: 找不到有效的传送门框架")
            return
        }
        
        val (frameBlocks, direction, width, height, bottomLeft) = result
        Blasphemy.Companion.logger.info("自定义传送门: 找到传送门框架，方向=$direction, 宽度=$width, 高度=$height")
        
        // 检查框架方块是否都是允许的方块
        for (framePos in frameBlocks) {
            val blockId = Registries.BLOCK.getId(world.getBlockState(framePos).block).toString()
            if (!portalBlocks.contains(blockId)) {
                Blasphemy.Companion.logger.info("自定义传送门: 框架使用了非法方块 $blockId")
                player.sendMessage(Text.translatable(config.messages.invalidBlock), true)
                cir.returnValue = ActionResult.FAIL
                return
            }
        }
        
        // 创建传送门方块
        createPortal(world, direction, width, height, bottomLeft)
        Blasphemy.Companion.logger.info("自定义传送门: 传送门创建成功")
        
        // 消耗物品耐久度（如果不是创造模式）
        if (!player.abilities.creativeMode) {
            context.stack.damage(1, player) { it.sendToolBreakStatus(context.hand) }
        }
        
        // 返回成功
        cir.returnValue = ActionResult.success(world.isClient)
    }
    
    /**
     * 检查是否存在标准尺寸的传送门框架（高度=5，宽度=4）
     */
    private fun checkStandardPortal(world: World, clickPos: BlockPos): PortalResult? {
        // 标准尺寸参数
        val PORTAL_HEIGHT = 5
        val PORTAL_WIDTH = 4
        
        // 找到底部边缘
        var basePos = clickPos
        while (isFrameBlock(world, basePos.down())) {
            basePos = basePos.down()
        }
        
        Blasphemy.Companion.logger.info("自定义传送门: 底部位置 ${basePos.x}, ${basePos.y}, ${basePos.z}")
        
        // 检查两个方向（Z轴和X轴）
        for (direction in arrayOf(Direction.NORTH, Direction.EAST)) {
            Blasphemy.Companion.logger.info("自定义传送门: 检查方向 $direction")
            
            // 寻找左下角
            var leftMoves = 0
            var bottomLeft = basePos
            
            // 根据方向选择"左"的方向
            val leftDirection = if (direction == Direction.NORTH) Direction.WEST else Direction.SOUTH
            
            // 向左移动直到找到左边缘
            while (isFrameBlock(world, bottomLeft.offset(leftDirection)) && leftMoves < PORTAL_WIDTH) {
                bottomLeft = bottomLeft.offset(leftDirection)
                leftMoves++
            }
            
            Blasphemy.Companion.logger.info("自定义传送门: 可能的左下角 ${bottomLeft.x}, ${bottomLeft.y}, ${bottomLeft.z}")
            
            // 验证底部边缘
            val frameBlocks = mutableListOf<BlockPos>()
            var isValidFrame = true
            
            // 检查底部
            for (x in 0 until PORTAL_WIDTH) {
                val checkPos = if (x == 0) bottomLeft else bottomLeft.offset(direction, x)
                if (!isFrameBlock(world, checkPos)) {
                    Blasphemy.Companion.logger.info("自定义传送门: 底部框架不完整，位置 ${checkPos.x}, ${checkPos.y}, ${checkPos.z}")
                    isValidFrame = false
                    break
                }
                frameBlocks.add(checkPos)
            }
            
            if (!isValidFrame) continue
            
            // 检查两侧边缘
            for (y in 1 until PORTAL_HEIGHT - 1) {
                // 左侧
                val leftSide = bottomLeft.offset(Direction.UP, y)
                if (!isFrameBlock(world, leftSide)) {
                    Blasphemy.Companion.logger.info("自定义传送门: 左边框架不完整，位置 ${leftSide.x}, ${leftSide.y}, ${leftSide.z}")
                    isValidFrame = false
                    break
                }
                frameBlocks.add(leftSide)
                
                // 右侧
                val rightSide = bottomLeft.offset(direction, PORTAL_WIDTH - 1).offset(Direction.UP, y)
                if (!isFrameBlock(world, rightSide)) {
                    Blasphemy.Companion.logger.info("自定义传送门: 右边框架不完整，位置 ${rightSide.x}, ${rightSide.y}, ${rightSide.z}")
                    isValidFrame = false
                    break
                }
                frameBlocks.add(rightSide)
            }
            
            if (!isValidFrame) continue
            
            // 检查顶部边缘
            for (x in 0 until PORTAL_WIDTH) {
                val topPos = bottomLeft.offset(direction, x).offset(Direction.UP, PORTAL_HEIGHT - 1)
                if (!isFrameBlock(world, topPos)) {
                    Blasphemy.Companion.logger.info("自定义传送门: 顶部框架不完整，位置 ${topPos.x}, ${topPos.y}, ${topPos.z}")
                    isValidFrame = false
                    break
                }
                frameBlocks.add(topPos)
            }
            
            if (!isValidFrame) continue
            
            // 检查内部是否为空气
            for (x in 1 until PORTAL_WIDTH) {
                for (y in 1 until PORTAL_HEIGHT - 1) {
                    val innerPos = bottomLeft.offset(direction, x).offset(Direction.UP, y)
                    if (!world.getBlockState(innerPos).isAir) {
                        Blasphemy.Companion.logger.info("自定义传送门: 内部不是空气，位置 ${innerPos.x}, ${innerPos.y}, ${innerPos.z}")
                        isValidFrame = false
                        break
                    }
                }
                if (!isValidFrame) break
            }
            
            if (!isValidFrame) continue
            
            Blasphemy.Companion.logger.info("自定义传送门: 找到有效的传送门框架！")
            return PortalResult(frameBlocks, direction, PORTAL_WIDTH, PORTAL_HEIGHT, bottomLeft)
        }
        
        return null
    }
    
    /**
     * 判断指定位置的方块是否可以作为传送门框架
     */
    private fun isFrameBlock(world: World, pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        val blockId = Registries.BLOCK.getId(block).toString()
        val allowedBlocks = ModConfig.getConfig().portalConfig.portalBlocks
        
        return allowedBlocks.contains(blockId)
    }
    
    /**
     * 创建传送门方块
     */
    private fun createPortal(world: World, direction: Direction, width: Int, height: Int, bottomLeft: BlockPos) {
        val axis = if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            Direction.Axis.Z
        } else {
            Direction.Axis.X
        }
        
        Blasphemy.Companion.logger.info("自定义传送门: 开始创建传送门方块，起始点=${bottomLeft.x},${bottomLeft.y},${bottomLeft.z}")
        
        // 在内部空间创建传送门方块
        for (x in 1 until width) {
            for (y in 1 until height - 1) {
                val portalPos = bottomLeft.offset(direction, x).offset(Direction.UP, y)
                Blasphemy.Companion.logger.info("自定义传送门: 在 ${portalPos.x},${portalPos.y},${portalPos.z} 创建传送门方块")
                val portalState = Blocks.NETHER_PORTAL.defaultState.with(NetherPortalBlock.AXIS, axis)
                world.setBlockState(portalPos, portalState)
            }
        }
    }
    
    /**
     * 传送门检测结果数据类
     */
    data class PortalResult(
        val frameBlocks: List<BlockPos>,  // 框架方块位置列表
        val direction: Direction,         // 框架方向
        val width: Int,                   // 框架宽度
        val height: Int,                  // 框架高度
        val bottomLeft: BlockPos          // 左下角位置
    )
} 