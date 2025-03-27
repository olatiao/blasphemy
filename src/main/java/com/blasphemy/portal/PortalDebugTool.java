package com.blasphemy.portal;

import com.blasphemy.Blasphemy;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * 传送门调试工具
 * 用于获取传送门框架信息和调试问题
 */
public class PortalDebugTool extends Item {
    public static final String ID = "portal_debug_tool";

    public PortalDebugTool() {
        super(new FabricItemSettings().maxCount(1));
    }

    /**
     * 注册传送门调试工具
     */
    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(Blasphemy.MOD_ID, ID), new PortalDebugTool());
        Blasphemy.LOGGER.info("注册传送门调试工具");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        Hand hand = context.getHand();

        if (world.isClient || player == null) {
            return ActionResult.SUCCESS;
        }

        if (hand == Hand.OFF_HAND) {
            return ActionResult.PASS;
        }

        // 检查方块是否是有效的框架方块
        if (PortalFrameValidator.isValidFrameBlock(world, pos)) {
            player.sendMessage(Text.literal("§a这是一个有效的传送门框架方块!"), false);
        } else {
            player.sendMessage(Text.literal("§c这不是一个有效的传送门框架方块。"), false);
            return ActionResult.SUCCESS;
        }

        // 尝试检测传送门
        PortalFrameValidator.PortalFrameResult result = PortalFrameValidator.validatePortalFrame(world, pos);

        if (result != null) {
            player.sendMessage(Text.literal("§a检测到有效的传送门框架！"), false);
            sendFrameInfo(player, result);

            // 如果玩家潜行，自动点亮传送门
            if (player.isSneaking()) {
                PortalFrameValidator.createPortal(world, result);
                player.sendMessage(Text.literal("§a已自动点亮传送门！"), false);
            } else {
                player.sendMessage(Text.literal("§e潜行点击来点亮传送门。"), false);
            }
        } else {
            player.sendMessage(Text.literal("§c未检测到有效的传送门框架。").formatted(Formatting.RED), false);

            // 尝试提供额外诊断信息
            player.sendMessage(Text.literal("§e开始诊断..."), false);

            // 检查方向
            for (Direction direction : Direction.Type.HORIZONTAL) {
                Direction.Axis axis = direction.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
                Direction sideDirection = Direction.from(axis, Direction.AxisDirection.POSITIVE);

                // 尝试找到框架的左下角
                BlockPos bottomLeft = tryFindBottomLeft(world, pos, sideDirection);
                if (bottomLeft != null) {
                    int width = measureWidth(world, bottomLeft, sideDirection);
                    int height = measureHeight(world, bottomLeft);

                    player.sendMessage(Text.literal(String.format(
                            "§e方向=%s: 找到可能的框架，位置=(%d,%d,%d), 宽度=%d, 高度=%d",
                            direction, bottomLeft.getX(), bottomLeft.getY(), bottomLeft.getZ(), width, height)), false);

                    // 检查大小是否符合要求
                    if (width < PortalFrameValidator.MIN_PORTAL_WIDTH
                            || height < PortalFrameValidator.MIN_PORTAL_HEIGHT) {
                        player.sendMessage(Text.literal(String.format(
                                "§c框架太小! 最小需要%dx%d，当前为%dx%d",
                                PortalFrameValidator.MIN_PORTAL_WIDTH, PortalFrameValidator.MIN_PORTAL_HEIGHT, width,
                                height)), false);
                    } else if (width > PortalFrameValidator.MAX_PORTAL_WIDTH
                            || height > PortalFrameValidator.MAX_PORTAL_HEIGHT) {
                        player.sendMessage(Text.literal(String.format(
                                "§c框架太大! 最大允许%dx%d，当前为%dx%d",
                                PortalFrameValidator.MAX_PORTAL_WIDTH, PortalFrameValidator.MAX_PORTAL_HEIGHT, width,
                                height)), false);
                    }

                    // 验证四个角落
                    BlockPos bottomRight = bottomLeft.offset(sideDirection, width - 1);
                    BlockPos topLeft = bottomLeft.offset(Direction.UP, height - 1);
                    BlockPos topRight = topLeft.offset(sideDirection, width - 1);

                    boolean hasCorners = true;
                    if (!PortalFrameValidator.isValidFrameBlock(world, bottomLeft)) {
                        player.sendMessage(Text.literal("§c左下角缺少框架方块!"), false);
                        hasCorners = false;
                    }
                    if (!PortalFrameValidator.isValidFrameBlock(world, bottomRight)) {
                        player.sendMessage(Text.literal("§c右下角缺少框架方块!"), false);
                        hasCorners = false;
                    }
                    if (!PortalFrameValidator.isValidFrameBlock(world, topLeft)) {
                        player.sendMessage(Text.literal("§c左上角缺少框架方块!"), false);
                        hasCorners = false;
                    }
                    if (!PortalFrameValidator.isValidFrameBlock(world, topRight)) {
                        player.sendMessage(Text.literal("§c右上角缺少框架方块!"), false);
                        hasCorners = false;
                    }

                    if (hasCorners) {
                        player.sendMessage(Text.literal("§a四个角落验证通过!"), false);

                        // 检查内部空间
                        boolean hasInvalidInner = false;
                        for (int h = 1; h < height - 1 && !hasInvalidInner; h++) {
                            for (int w = 1; w < width - 1 && !hasInvalidInner; w++) {
                                BlockPos innerPos = bottomLeft.offset(Direction.UP, h).offset(sideDirection, w);
                                if (!world.getBlockState(innerPos).isAir()) {
                                    player.sendMessage(Text.literal(String.format(
                                            "§c内部空间被占用! 位置=(%d,%d,%d)",
                                            innerPos.getX(), innerPos.getY(), innerPos.getZ())), false);
                                    hasInvalidInner = true;
                                }
                            }
                        }

                        if (!hasInvalidInner) {
                            player.sendMessage(Text.literal("§a内部空间验证通过，框架应该是有效的！请尝试在不同位置使用调试工具。"), false);
                        }
                    }
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    /**
     * 显示框架信息
     */
    private void sendFrameInfo(PlayerEntity player, PortalFrameValidator.PortalFrameResult result) {
        player.sendMessage(Text.literal(String.format(
                "§e框架信息: 宽度=%d, 高度=%d, 方向=%s",
                result.width, result.height, result.direction)), false);

        player.sendMessage(Text.literal(String.format(
                "§e左下角位置: (%d, %d, %d)",
                result.bottomLeft.getX(), result.bottomLeft.getY(), result.bottomLeft.getZ())), false);

        player.sendMessage(Text.literal(String.format(
                "§e框架方块数量: %d", result.frameBlocks.size())), false);
    }

    /**
     * 尝试找到框架的左下角
     */
    private BlockPos tryFindBottomLeft(World world, BlockPos startPos, Direction sideDirection) {
        // 如果起始位置不是框架方块，返回null
        if (!PortalFrameValidator.isValidFrameBlock(world, startPos)) {
            return null;
        }

        // 向下和向左寻找最远的框架方块
        BlockPos current = startPos;
        BlockPos next;

        // 向下找最底部的框架方块
        while (PortalFrameValidator.isValidFrameBlock(world, next = current.down())) {
            current = next;
        }

        // 向反方向找最左侧的框架方块
        Direction oppositeOfSide = sideDirection.getOpposite();
        while (PortalFrameValidator.isValidFrameBlock(world, next = current.offset(oppositeOfSide))) {
            current = next;
        }

        return current;
    }

    /**
     * 测量框架宽度
     */
    private int measureWidth(World world, BlockPos bottomLeft, Direction sideDirection) {
        int width = 1;
        BlockPos current = bottomLeft;

        // 沿着侧向方向测量到有效的框架边界
        while (width < PortalFrameValidator.MAX_PORTAL_WIDTH) {
            BlockPos next = current.offset(sideDirection);
            if (!PortalFrameValidator.isValidFrameBlock(world, next)) {
                break;
            }
            width++;
            current = next;
        }

        return width;
    }

    /**
     * 测量框架高度
     */
    private int measureHeight(World world, BlockPos bottomLeft) {
        int height = 1;
        BlockPos current = bottomLeft;

        // 沿着向上方向测量到有效的框架边界
        while (height < PortalFrameValidator.MAX_PORTAL_HEIGHT) {
            BlockPos next = current.offset(Direction.UP);
            if (!PortalFrameValidator.isValidFrameBlock(world, next)) {
                break;
            }
            height++;
            current = next;
        }

        return height;
    }
}