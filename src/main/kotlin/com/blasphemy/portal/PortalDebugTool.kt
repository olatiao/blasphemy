package com.blasphemy.portal

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/** 传送门调试工具，用于显示传送门框架信息 */
class PortalDebugTool : Item(FabricItemSettings().maxCount(1)) {

    companion object {
        private val ITEM_ID = Identifier(Blasphemy.MOD_ID, "portal_debug_tool")
        private val validator = PortalFrameValidator()

        /** 注册传送门调试工具 */
        fun register() {
            val tool = PortalDebugTool()
            Registry.register(Registries.ITEM, ITEM_ID, tool)
            Blasphemy.logger.info("传送门调试工具已注册")
        }
    }

    /** 当玩家右键点击方块时触发 */
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        val player = context.player ?: return ActionResult.PASS

        if (world.isClient) {
            return ActionResult.SUCCESS
        }

        Blasphemy.logger.info("传送门调试工具: 检查位置 ${pos.x}, ${pos.y}, ${pos.z}")

        // 创建验证器
        val validator = PortalFrameValidator()

        // 检查点击的方块是否是有效的传送门框架方块
        val block = world.getBlockState(pos).block
        val blockId = Registries.BLOCK.getId(block).toString()
        val isValidFrame = validator.isValidFrameBlock(world, pos)

        player.sendMessage(Text.literal("§e======== 传送门调试 ========"), false)
        player.sendMessage(Text.literal("§7方块: §f$blockId"), false)
        player.sendMessage(Text.literal("§7是否有效框架: §f$isValidFrame"), false)

        // 显示配置信息
        player.sendMessage(Text.literal("§e==== 传送门配置信息 ===="), false)
        
        // 获取并显示有效的传送门框架方块列表
        player.sendMessage(Text.literal("§7有效框架方块:"), false)
        val frameBlocks = PortalFrameValidator.getValidFrameBlocks()
        frameBlocks.forEach { id ->
            player.sendMessage(Text.literal("§7  * §f$id"), false)
        }
        
        // 显示点火物品
        val config = ModConfig.getConfig().portalConfig
        player.sendMessage(Text.literal("§7点火物品: §f${config.ignitionItem.ifEmpty { "minecraft:flint_and_steel (默认)" }}"), false)

        // 如果是有效的框架方块，尝试检测框架
        if (isValidFrame) {
            val result = validator.validateFrameStructure(world, pos)
            if (result != null) {
                player.sendMessage(Text.literal("§a检测到有效传送门框架:"), false)
                player.sendMessage(Text.literal("§7  方向: §f${result.direction}"), false)
                player.sendMessage(Text.literal("§7  框架尺寸(外部): §f${result.width}×${result.height}"), false)
                player.sendMessage(Text.literal("§7  内部空间尺寸: §f${result.innerWidth}×${result.innerHeight}"), false)
                player.sendMessage(Text.literal("§7  总框架方块数: §f${result.frameBlocks.size}"), false)
                player.sendMessage(Text.literal("§7  底部左角: §f${result.bottomLeft.x}, ${result.bottomLeft.y}, ${result.bottomLeft.z}"), false)
                
                // 显示创建此框架可能需要的方块总数
                player.sendMessage(Text.literal("§7  需要框架方块数量: §f${2 * (result.width + result.height) - 4}"), false)
            } else {
                player.sendMessage(Text.literal("§c未检测到有效传送门框架"), false)
            }
        }

        return ActionResult.SUCCESS
    }

    /** 重写使用物品方法，右键空气时显示传送门配置信息 */
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        
        if (world.isClient) {
            return TypedActionResult.success(stack)
        }
        
        user.sendMessage(Text.literal("§e======== 传送门配置 ========"), false)
        
        // 显示传送门配置状态
        val config = ModConfig.getConfig().portalConfig
        user.sendMessage(Text.literal("§7启用状态: §f${config.enabled}"), false)
        
        // 显示最小和最大尺寸
        user.sendMessage(Text.literal("§7最小内部尺寸: §f${PortalFrameValidator.MIN_PORTAL_WIDTH}×${PortalFrameValidator.MIN_PORTAL_HEIGHT}"), false)
        user.sendMessage(Text.literal("§7最大内部尺寸: §f${PortalFrameValidator.MAX_PORTAL_SIZE}×${PortalFrameValidator.MAX_PORTAL_SIZE}"), false)
        
        // 显示有效的传送门框架方块列表
        user.sendMessage(Text.literal("§7有效框架方块:"), false)
        val frameBlocks = PortalFrameValidator.getValidFrameBlocks()
        frameBlocks.forEach { id ->
            user.sendMessage(Text.literal("§7  * §f$id"), false)
        }
        
        // 显示点火物品
        user.sendMessage(Text.literal("§7点火物品: §f${config.ignitionItem.ifEmpty { "minecraft:flint_and_steel (默认)" }}"), false)
        
        return TypedActionResult.success(stack)
    }
}
