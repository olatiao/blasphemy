package com.blasphemy.mixin;

import com.blasphemy.Blasphemy;
import com.blasphemy.config.ModConfig;
import com.blasphemy.portal.PortalFrameValidator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 火焰弹点燃机制的Mixin
 * 拦截火焰弹的使用，以便应用自定义传送门规则
 */
@Mixin(FireChargeItem.class)
public class FireChargeMixin {
    /**
     * 拦截火焰弹的使用，应用自定义传送门规则
     */
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseFireCharge(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        Blasphemy.LOGGER.info("火焰弹使用事件被拦截，开始检查传送门点燃条件");
        
        // 检查是否启用传送门功能
        if (!ModConfig.getConfig().portalConfig.enabled) {
            Blasphemy.LOGGER.info("自定义传送门功能未启用，跳过处理");
            return;
        }
        
        // 检查是否点击的是黑曜石或有效框架方块
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = context.getWorld().getBlockState(blockPos);
        if (blockState.getBlock() != Blocks.OBSIDIAN && !PortalFrameValidator.isValidFrameBlock(context.getWorld(), blockPos)) {
            Blasphemy.LOGGER.info("点击的不是有效的传送门框架方块：{}", blockState.getBlock().getName().getString());
            return;
        }
        
        // 尝试使用自定义规则点燃传送门
        Blasphemy.LOGGER.info("尝试使用自定义规则点燃传送门");
        
        // 确保使用的物品与配置匹配 - 特殊情况：如果是火焰弹本身，即使配置的是其他物品，也允许点燃
        boolean isFireCharge = context.getStack().getItem() instanceof FireChargeItem;
        boolean isConfigItem = PortalFrameValidator.isValidIgnitionItem(context.getStack());
        
        Blasphemy.LOGGER.info("物品检查：isFireCharge={}, isConfigItem={}, configItem={}", 
            isFireCharge, isConfigItem, ModConfig.getConfig().portalConfig.ignitionItem);
            
        if (isFireCharge || isConfigItem) {
            if (PortalFrameValidator.tryIgnitePortal(context)) {
                Blasphemy.LOGGER.info("传送门成功点燃！阻止原版点火方法执行");
                // 如果成功点燃，阻止原版代码执行
                cir.setReturnValue(ActionResult.success(true));
            } else {
                Blasphemy.LOGGER.info("自定义传送门点燃失败，继续执行原版点火方法");
            }
        } else {
            Blasphemy.LOGGER.info("物品不匹配，继续执行原版点火方法");
        }
    }
} 