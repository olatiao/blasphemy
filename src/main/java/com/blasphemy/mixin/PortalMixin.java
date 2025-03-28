package com.blasphemy.mixin;

import com.blasphemy.Blasphemy;
import com.blasphemy.config.ModConfig;
import com.blasphemy.portal.PortalFrameValidator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 传送门点燃机制的Mixin
 * 拦截打火石的使用，以便应用自定义传送门规则
 */
@Mixin(FlintAndSteelItem.class)
public class PortalMixin {
    /**
     * 拦截打火石的使用，应用自定义传送门规则
     */
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseFlintAndSteel(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        Blasphemy.LOGGER.info("打火石使用事件被拦截，开始检查传送门点燃条件");

        // 检查是否启用传送门功能
        if (!ModConfig.getConfig().portalConfig.enabled) {
            Blasphemy.LOGGER.info("自定义传送门功能未启用，跳过处理");
            return;
        }

        // 检查使用的点火方块
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = context.getWorld().getBlockState(blockPos);

        // 记录点击的方块信息
        Blasphemy.LOGGER.info("点击的方块: {}, 位置: {}",
                blockState.getBlock().getName().getString(), blockPos);

        // 检查是否支持原版打火石
        boolean supportVanillaItems = ModConfig.getConfig().portalConfig.supportVanillaItems;

        // 如果不支持原版打火石，且使用的是打火石，且配置的点火物品不是打火石，阻止使用
        boolean isFlintAndSteel = context.getStack().getItem() instanceof FlintAndSteelItem;
        boolean isConfigItem = PortalFrameValidator.isValidIgnitionItem(context.getStack());

        Blasphemy.LOGGER.info("物品检查：isFlintAndSteel={}, isConfigItem={}, configItem={}, supportVanilla={}",
                isFlintAndSteel, isConfigItem, ModConfig.getConfig().portalConfig.ignitionItem, supportVanillaItems);

        // 如果不支持原版物品，且是使用打火石，但打火石不是配置的点火物品，阻止继续
        if (!supportVanillaItems && isFlintAndSteel && !isConfigItem) {
            Blasphemy.LOGGER.info("禁用原版打火石点燃传送门");
            cir.setReturnValue(ActionResult.PASS);
            return;
        }
        // 如果方块是黑曜石且配置中不存在黑曜石，阻止继续
        if (blockState.getBlock() == Blocks.OBSIDIAN
                && !ModConfig.getConfig().portalConfig.portalBlocks.contains("minecraft:obsidian")) {
            Blasphemy.LOGGER.info("阻止在黑曜石上使用打火石点燃原版传送门");
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        // 尝试使用自定义规则点燃传送门
        if (PortalFrameValidator.tryIgnitePortal(context)) {
            Blasphemy.LOGGER.info("传送门成功点燃！阻止原版点火方法执行");
            // 如果成功点燃，阻止原版代码执行
            cir.setReturnValue(ActionResult.success(true));
        } else {
            Blasphemy.LOGGER.info("自定义传送门点燃失败");

            // 如果是黑曜石并且配置中不允许黑曜石，阻止原版点火
            if (blockState.getBlock() == Blocks.OBSIDIAN) {
                // 是否允许黑曜石
                boolean obsidianAllowed = ModConfig.getConfig().portalConfig.portalBlocks
                        .contains("minecraft:obsidian");
                // 如果配置中不允许黑曜石，阻止原版点火
                if (!obsidianAllowed) {
                    Blasphemy.LOGGER.info("阻止在黑曜石上使用打火石点燃原版传送门");
                    cir.setReturnValue(ActionResult.PASS);
                }
            }
        }
    }
}