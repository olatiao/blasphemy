package com.blasphemy.mixin;

import com.blasphemy.Blasphemy;
import com.blasphemy.config.ModConfig;
import com.blasphemy.portal.PortalFrameValidator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通用物品的Mixin
 * 用于拦截所有物品的useOnBlock方法
 */
@Mixin(Item.class)
public class ItemMixin {
    
    /**
     * 拦截任何物品的使用，检查是否是自定义点火物品
     */
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void onUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        // 首先检查是否启用了传送门功能
        if (!ModConfig.getConfig().portalConfig.enabled) {
            return;
        }
        
        Item currentItem = context.getStack().getItem();
        
        // 跳过打火石和火焰弹，它们由专门的Mixin处理
        String flintAndSteel = "minecraft:flint_and_steel";
        String fireCharge = "minecraft:fire_charge";
        Item flintAndSteelItem = net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(flintAndSteel));
        Item fireChargeItem = net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(fireCharge));
        
        if (currentItem == flintAndSteelItem || currentItem == fireChargeItem) {
            return;
        }
        
        // 判断是否是配置的自定义点火物品
        String configuredItem = ModConfig.getConfig().portalConfig.ignitionItem;
        boolean isConfiguredItem = false;
        
        try {
            Item configItem = net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(configuredItem));
            isConfiguredItem = (currentItem == configItem);
        } catch (Exception e) {
            Blasphemy.LOGGER.error("无法解析配置的点火物品：{}", configuredItem);
        }
        
        if (!isConfiguredItem) {
            return;
        }
        
        Blasphemy.LOGGER.info("===== 自定义物品点火尝试 =====");
        Blasphemy.LOGGER.info("使用物品: {}, 配置物品: {}, 匹配: {}",
            currentItem.getName().getString(), configuredItem, isConfiguredItem);
        
        // 检查是否点击的是黑曜石或有效框架方块
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = context.getWorld().getBlockState(blockPos);
        
        if (blockState.getBlock() == Blocks.OBSIDIAN || 
            PortalFrameValidator.isValidFrameBlock(context.getWorld(), blockPos)) {
            
            // 记录点击的方块信息
            Blasphemy.LOGGER.info("点击的方块: {}, 位置: {}", 
                blockState.getBlock().getName().getString(), blockPos);
            
            // 尝试使用自定义规则点燃传送门
            Blasphemy.LOGGER.info("尝试使用自定义物品点燃传送门");
            
            boolean result = PortalFrameValidator.tryIgnitePortal(context);
            if (result) {
                Blasphemy.LOGGER.info("传送门成功点燃！阻止原版点火方法执行");
                // 如果成功点燃，阻止原版代码执行
                cir.setReturnValue(ActionResult.success(true));
            } else {
                Blasphemy.LOGGER.info("自定义传送门点燃失败");
            }
        } else {
            Blasphemy.LOGGER.info("点击的不是有效的框架方块: {}", blockState.getBlock().getName().getString());
        }
    }
} 