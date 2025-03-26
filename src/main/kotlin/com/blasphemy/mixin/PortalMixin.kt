package com.blasphemy.mixin

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import com.blasphemy.portal.PortalFrameValidator
import net.minecraft.block.Blocks
import net.minecraft.block.NetherPortalBlock
import net.minecraft.item.FlintAndSteelItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** 
 * 传送门创建Mixin，用于控制传送门的创建逻辑 
 * 注意：此处修改为混入Item类，使所有物品都能尝试点亮传送门
 */
@Mixin(Item::class)
class PortalMixin {
    
    // 传送门框架验证器实例
    private val portalValidator = PortalFrameValidator()

    @Inject(method = ["useOnBlock"], at = [At("HEAD")], cancellable = true)
    private fun useOnBlock(context: ItemUsageContext, cir: CallbackInfoReturnable<ActionResult>) {
        val player = context.player ?: return
        val world = context.world

        // 客户端不处理
        if (world.isClient) {
            return
        }

        val pos = context.blockPos 
        val side = context.side
        val hitPos = context.blockPos.offset(side)
        val itemStack = context.stack

        // 获取配置
        val config = ModConfig.getConfig().portalConfig

        // 如果未启用自定义传送门，直接返回
        if (!config.enabled) {
            return
        }
        
        // 检查物品ID
        val itemId = Registries.ITEM.getId(itemStack.item).toString()
        if (!PortalFrameValidator.isValidIgnitionItem(itemId)) {
            // 不是传送门点火物品，直接跳过处理
            return
        }

        Blasphemy.logger.info("自定义传送门: 使用物品 $itemId 在位置 ${pos.x}, ${pos.y}, ${pos.z}")

        // 检查点击的是不是允许的传送门框架方块
        if (!portalValidator.isValidFrameBlock(world, pos)) {
            Blasphemy.logger.info("自定义传送门: 点击的不是允许的框架方块")
            return
        }

        Blasphemy.logger.info("自定义传送门: 点击了可能的传送门框架方块")

        // 使用传送门验证器检测传送门框架
        val result = portalValidator.validateFrameStructure(world, pos)
        if (result == null) {
            Blasphemy.logger.info("自定义传送门: 找不到有效的传送门框架")
            player.sendMessage(Text.translatable(config.messages.invalidBlock), true)
            return
        }

        Blasphemy.logger.info("自定义传送门: 找到传送门框架，方向=${result.direction}, 宽度=${result.width}, 高度=${result.height}")

        // 创建传送门方块
        portalValidator.createPortal(world, result)
        Blasphemy.logger.info("自定义传送门: 传送门创建成功")

        // 消耗物品耐久度或数量（如果不是创造模式）
        if (!player.abilities.creativeMode) {
            val hasChanged = consumeItem(itemStack, player, context)
            if (hasChanged) {
                player.sendMessage(Text.translatable(config.messages.portalCreated), true)
            }
        } else {
            player.sendMessage(Text.translatable(config.messages.portalCreated), true)
        }

        // 返回成功
        cir.returnValue = ActionResult.success(world.isClient)
    }
    
    /**
     * 消耗物品耐久或数量
     * @return 如果物品被消耗或耐久度减少则返回true
     */
    private fun consumeItem(itemStack: ItemStack, player: net.minecraft.entity.player.PlayerEntity, context: ItemUsageContext): Boolean {
        // 检查物品是否有耐久度
        if (itemStack.isDamageable) {
            // 减少耐久度
            itemStack.damage(1, player) { it.sendToolBreakStatus(context.hand) }
            return true
        } else {
            // 没有耐久度，减少数量
            itemStack.decrement(1)
            return true
        }
    }
}
