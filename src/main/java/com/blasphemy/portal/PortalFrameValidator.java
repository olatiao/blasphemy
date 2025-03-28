package com.blasphemy.portal;

import com.blasphemy.Blasphemy;
import com.blasphemy.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 传送门框架验证器
 * 用于验证和创建自定义传送门
 */
public class PortalFrameValidator {

    // 传送门尺寸限制
    public static final int MIN_PORTAL_WIDTH = 4; // 外框最小宽度（内部空间为2）
    public static final int MIN_PORTAL_HEIGHT = 5; // 外框最小高度（内部空间为3）
    public static final int MAX_PORTAL_WIDTH = 23; // 外框最大宽度
    public static final int MAX_PORTAL_HEIGHT = 23; // 外框最大高度

    /**
     * 初始化验证器
     * 从配置中加载有效的框架方块
     */
    public static void init() {
        Blasphemy.LOGGER.info("初始化传送门框架验证器");
    }

    /**
     * 检查物品是否是有效的点火物品
     */
    public static boolean isValidIgnitionItem(ItemStack stack) {
        if (stack.isEmpty()) {
            Blasphemy.LOGGER.debug("点火物品检查：物品为空");
            return false;
        }

        // 基本的匹配检查
        boolean isValid = false;
        // 获取配置中的点火物品
        String configItemId = ModConfig.getConfig().portalConfig.ignitionItem;

        // 检查是否匹配配置中的物品
        if (configItemId != null && stack.getItem() == Registries.ITEM.get(new Identifier(configItemId))) {
            Blasphemy.LOGGER.info("点火物品检查：物品 {} 匹配配置物品 {}",
                    stack.getItem().getName().getString(), configItemId);
            isValid = true;
        }

        // 如果未启用自定义，或明确设置支持原版，则支持默认物品
        if (!ModConfig.getConfig().portalConfig.enabled ||
                ModConfig.getConfig().portalConfig.supportVanillaItems) {

            // 检查是否是打火石
            String flintAndSteel = "minecraft:flint_and_steel";
            Item flintAndSteelItem = Registries.ITEM.get(new Identifier(flintAndSteel));
            if (stack.getItem() == flintAndSteelItem) {
                Blasphemy.LOGGER.info("点火物品检查：物品 {} 是打火石", stack.getItem().getName().getString());
                isValid = true;
            }

            // 检查是否是火焰弹
            String fireCharge = "minecraft:fire_charge";
            Item fireChargeItem = Registries.ITEM.get(new Identifier(fireCharge));
            if (stack.getItem() == fireChargeItem) {
                Blasphemy.LOGGER.info("点火物品检查：物品 {} 是火焰弹", stack.getItem().getName().getString());
                isValid = true;
            }
        }

        Blasphemy.LOGGER.info("点火物品检查结果：物品={}, 配置项={}, 结果={}",
                stack.getItem().getName().getString(), configItemId, isValid ? "有效" : "无效");

        return isValid;
    }

    /**
     * 检查方块是否是有效的框架方块
     */
    public static boolean isValidFrameBlock(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }

        // 获取点击的方块
        BlockState state = world.getBlockState(pos);
        // 获取方块
        Block block = state.getBlock();
        // 获取方块id
        String blockId = Registries.BLOCK.getId(block).toString();
        // 获取配置文件中允许的框架方块
        List<String> allowedBlocks = ModConfig.getConfig().portalConfig.portalBlocks;

        // 检查是否在配置的有效框架方块列表中
        if (allowedBlocks.contains(blockId)) {
            Blasphemy.LOGGER.info("方块 {} 在配置列表中，是有效的框架方块", blockId);
            return true;
        }

        // 黑曜石特殊处理
        if (block == Blocks.OBSIDIAN && !allowedBlocks.contains("minecraft:obsidian")) {
            if (!ModConfig.getConfig().portalConfig.supportVanillaItems) {
                Blasphemy.LOGGER.debug("黑曜石方块检查：不在配置列表中，不是有效框架方块");
                return false;
            }
        }

        return false;
    }

    /**
     * 尝试点燃传送门
     * 
     * @return 是否成功点燃
     */
    public static boolean tryIgnitePortal(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (world.isClient || player == null) {
            return false;
        }

        Blasphemy.LOGGER.info("======== 传送门点燃尝试 ========");
        Blasphemy.LOGGER.info("玩家 {} 尝试在 {} 点燃传送门", player.getName().getString(), pos);
        Blasphemy.LOGGER.info("使用物品: {}", context.getStack().getItem().getName().getString());

        // 记录点击的方块类型
        BlockState blockState = world.getBlockState(pos);
        Block clickedBlock = blockState.getBlock();
        Blasphemy.LOGGER.info("点击方块: {}", clickedBlock.getName().getString());

        // 首先检查点击的方块是否是有效的框架方块
        if (!isValidFrameBlock(world, pos)) {
            String invalidBlockMsg = ModConfig.getConfig().portalConfig.messages.invalidBlock;
            player.sendMessage(Text.translatable(invalidBlockMsg).formatted(Formatting.RED), true);
            Blasphemy.LOGGER.info("点火失败：点击的不是有效的框架方块 {}", blockState.getBlock().getName().getString());
            return false;
        }

        Blasphemy.LOGGER.info("配置状态: enabled={}, supportVanillaItems={}, configItem={}",
                ModConfig.getConfig().portalConfig.enabled,
                ModConfig.getConfig().portalConfig.supportVanillaItems,
                ModConfig.getConfig().portalConfig.ignitionItem);

        // 检查是否是有效的点火物品
        ItemStack stack = context.getStack();
        if (!isValidIgnitionItem(stack)) {
            Blasphemy.LOGGER.info("点火失败：不是有效的点火物品");
            if (player != null) {
                String invalidItemMsg = ModConfig.getConfig().portalConfig.messages.invalidItem;
                player.sendMessage(Text.translatable(invalidItemMsg).formatted(Formatting.RED), true);
            }
            return false;
        }

        // 验证传送门框架
        Blasphemy.LOGGER.info("开始验证传送门框架...");
        PortalFrameResult result = validatePortalFrame(world, pos);
        if (result == null) {
            Blasphemy.LOGGER.info("点火失败：未找到有效的传送门框架");
            if (player != null) {
                player.sendMessage(Text.literal("未找到有效的传送门框架").formatted(Formatting.RED), true);
            }
            return false;
        }

        // 创建传送门
        Blasphemy.LOGGER.info("找到有效的传送门框架，尺寸：{}x{}，方向：{}", result.width, result.height, result.direction);
        createPortal(world, result);

        // 损耗物品（如果不是创造模式）
        if (!player.isCreative()) {
            if (stack.isDamageable()) {
                stack.damage(1, player, p -> p.sendToolBreakStatus(context.getHand()));
            } else {
                stack.decrement(1);
            }
        }

        Blasphemy.LOGGER.info("传送门点燃成功");
        if (player != null) {
            player.sendMessage(Text.literal("传送门已激活！").formatted(Formatting.GREEN), true);
        }
        return true;
    }

    /**
     * 验证传送门框架
     * 
     * @return 如果有效，返回框架结果；否则返回null
     */
    public static PortalFrameResult validatePortalFrame(World world, BlockPos pos) {
        if (world == null || pos == null) {
            Blasphemy.LOGGER.warn("验证传送门框架失败：世界或位置为空");
            return null;
        }

        Blasphemy.LOGGER.info("开始验证传送门框架，位置：{}", pos);

        // 验证点击位置是否是有效的框架方块
        if (!isValidFrameBlock(world, pos)) {
            Blasphemy.LOGGER.info("点击的不是有效的框架方块");
            return null;
        }

        // 检查两个可能的朝向（X轴和Z轴）
        PortalFrameResult xResult = validatePortalFrameOnAxis(world, pos, Direction.Axis.X);
        if (xResult != null) {
            Blasphemy.LOGGER.info("找到X轴方向的有效传送门框架");
            return xResult;
        }

        PortalFrameResult zResult = validatePortalFrameOnAxis(world, pos, Direction.Axis.Z);
        if (zResult != null) {
            Blasphemy.LOGGER.info("找到Z轴方向的有效传送门框架");
            return zResult;
        }

        Blasphemy.LOGGER.info("未找到有效的传送门框架");
        return null;
    }

    /**
     * 在指定轴上验证传送门框架
     */
    private static PortalFrameResult validatePortalFrameOnAxis(World world, BlockPos pos, Direction.Axis axis) {
        // 尝试查找框架底部
        BlockPos bottomFrame = findBaseFrame(world, pos, axis);
        if (bottomFrame == null) {
            Blasphemy.LOGGER.info("{}轴：未找到底部框架", axis);
            return null;
        }

        Blasphemy.LOGGER.info("{}轴：找到底部框架在 {}", axis, bottomFrame);

        // 定义宽度和高度方向
        Direction widthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction heightDir = Direction.UP;

        // 测量框架大小
        Size size = computePortalSize(world, bottomFrame, widthDir, heightDir);

        if (size == null || !size.isValid()) {
            Blasphemy.LOGGER.info("{}轴：框架大小无效", axis);
            return null;
        }

        Blasphemy.LOGGER.info("{}轴：有效框架大小 {}x{}", axis, size.width, size.height);

        // 创建结果
        PortalFrameResult result = new PortalFrameResult();
        result.width = size.width;
        result.height = size.height;
        result.direction = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        result.bottomLeft = size.corner;
        result.frameBlocks = size.frameBlocks;

        Blasphemy.LOGGER.info("验证传送门框架成功：{}x{}，方向：{}，位置：{}",
                result.width, result.height, result.direction, result.bottomLeft);

        return result;
    }

    /**
     * 查找基础框架位置
     */
    private static BlockPos findBaseFrame(World world, BlockPos pos, Direction.Axis axis) {
        // 向下查找
        BlockPos current = pos;
        while (current.getY() > world.getBottomY() && isValidFrameBlock(world, current.down())) {
            current = current.down();
        }

        // 现在查找最左侧
        Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        while (isValidFrameBlock(world, current.offset(left))) {
            current = current.offset(left);
        }

        return current;
    }

    /**
     * 计算传送门大小
     */
    private static Size computePortalSize(World world, BlockPos corner, Direction widthDir, Direction heightDir) {
        Size size = new Size(world, corner, widthDir, heightDir);

        // 计算传送门大小
        size.calculatePortalSize();

        if (!size.isValid()) {
            Blasphemy.LOGGER.info("计算的传送门大小无效：{}x{}", size.width, size.height);
            return null;
        }

        return size;
    }

    /**
     * 传送门大小计算器
     * 模拟原版逻辑
     */
    private static class Size {
        private final World world;
        private final Direction.Axis axis;
        private final Direction rightDir;
        private final Direction downDir;
        private int width;
        private int height;
        private BlockPos corner;
        private Set<BlockPos> frameBlocks = new HashSet<>();
        private boolean foundPortal = false;

        public Size(World world, BlockPos pos, Direction rightDir, Direction downDir) {
            this.world = world;
            this.axis = rightDir.getAxis();
            this.rightDir = rightDir;
            this.downDir = downDir;
            this.corner = pos;
        }

        /**
         * 计算传送门大小
         */
        public void calculatePortalSize() {
            // 如果初始位置向下是基岩等框架不能穿过的方块，直接失败
            if (downDir == Direction.DOWN && corner.getY() <= world.getBottomY()) {
                Blasphemy.LOGGER.info("传送门计算：初始位置在世界底部，无法形成框架");
                foundPortal = false;
                return;
            }

            // 计算宽度
            width = 0;
            while (width < MAX_PORTAL_WIDTH && isValidFrameBlock(world, corner.offset(rightDir, width + 1))) {
                width++;
            }

            if (width < MIN_PORTAL_WIDTH - 1) { // 最小宽度
                Blasphemy.LOGGER.info("传送门计算：宽度太小，宽度={}", width + 1);
                foundPortal = false;
                return;
            }

            // 计算高度
            height = 0;
            while (height < MAX_PORTAL_HEIGHT && isValidFrameBlock(world, corner.offset(downDir, height + 1))) {
                height++;
            }

            if (height < MIN_PORTAL_HEIGHT - 1) { // 最小高度
                Blasphemy.LOGGER.info("传送门计算：高度太小，高度={}", height + 1);
                foundPortal = false;
                return;
            }

            Blasphemy.LOGGER.info("传送门计算：初始大小 {}x{}", width + 1, height + 1);

            // 收集帧方块并验证
            if (!collectFrameBlocks()) {
                Blasphemy.LOGGER.info("传送门计算：收集框架方块失败");
                foundPortal = false;
                return;
            }

            // 验证内部空间
            if (!validateInnerSpace()) {
                Blasphemy.LOGGER.info("传送门计算：内部空间验证失败");
                foundPortal = false;
                return;
            }

            // 传送门大小正确
            width += 1; // 转换为实际宽度
            height += 1; // 转换为实际高度
            foundPortal = true;

            Blasphemy.LOGGER.info("传送门计算：成功，大小={}x{}", width, height);
        }

        /**
         * 收集框架方块
         */
        private boolean collectFrameBlocks() {
            frameBlocks.clear();

            // 检查四个角落
            BlockPos tlCorner = corner;
            BlockPos trCorner = corner.offset(rightDir, width);
            BlockPos blCorner = corner.offset(downDir, height);
            BlockPos brCorner = corner.offset(rightDir, width).offset(downDir, height);

            // 所有角落必须是有效框架方块
            if (!isValidFrameBlock(world, tlCorner)) {
                Blasphemy.LOGGER.info("传送门计算：左上角框架缺失");
                return false;
            }
            frameBlocks.add(tlCorner);

            if (!isValidFrameBlock(world, trCorner)) {
                Blasphemy.LOGGER.info("传送门计算：右上角框架缺失");
                return false;
            }
            frameBlocks.add(trCorner);

            if (!isValidFrameBlock(world, blCorner)) {
                Blasphemy.LOGGER.info("传送门计算：左下角框架缺失");
                return false;
            }
            frameBlocks.add(blCorner);

            if (!isValidFrameBlock(world, brCorner)) {
                Blasphemy.LOGGER.info("传送门计算：右下角框架缺失");
                return false;
            }
            frameBlocks.add(brCorner);

            // 处理底部边缘（除角落外）
            for (int w = 1; w < width; w++) {
                BlockPos bottomPos = corner.offset(rightDir, w).offset(downDir, height);
                if (!isValidFrameBlock(world, bottomPos)) {
                    Blasphemy.LOGGER.info("传送门计算：底部框架缺失 在 {}", bottomPos);
                    return false;
                }
                frameBlocks.add(bottomPos);
            }

            // 处理顶部边缘（除角落外）
            for (int w = 1; w < width; w++) {
                BlockPos topPos = corner.offset(rightDir, w);
                if (!isValidFrameBlock(world, topPos)) {
                    Blasphemy.LOGGER.info("传送门计算：顶部框架缺失 在 {}", topPos);
                    return false;
                }
                frameBlocks.add(topPos);
            }

            // 处理左边边缘（除角落外）
            for (int h = 1; h < height; h++) {
                BlockPos leftPos = corner.offset(downDir, h);
                if (!isValidFrameBlock(world, leftPos)) {
                    Blasphemy.LOGGER.info("传送门计算：左边框架缺失 在 {}", leftPos);
                    return false;
                }
                frameBlocks.add(leftPos);
            }

            // 处理右边边缘（除角落外）
            for (int h = 1; h < height; h++) {
                BlockPos rightPos = corner.offset(rightDir, width).offset(downDir, h);
                if (!isValidFrameBlock(world, rightPos)) {
                    Blasphemy.LOGGER.info("传送门计算：右边框架缺失 在 {}", rightPos);
                    return false;
                }
                frameBlocks.add(rightPos);
            }

            Blasphemy.LOGGER.info("传送门计算：框架方块总数={}", frameBlocks.size());
            return true; // 所有边缘检查通过
        }

        /**
         * 验证内部空间
         */
        private boolean validateInnerSpace() {
            // 检查内部是否全是空气
            boolean allAir = true;
            for (int h = 1; h < height; h++) {
                for (int w = 1; w < width; w++) {
                    BlockPos innerPos = corner.offset(rightDir, w).offset(downDir, h);
                    if (!world.getBlockState(innerPos).isAir()) {
                        Blasphemy.LOGGER.info("传送门计算：内部空间被占用 在 {}", innerPos);
                        allAir = false;
                        break;
                    }
                }
                if (!allAir)
                    break;
            }

            if (!allAir) {
                Blasphemy.LOGGER.info("传送门计算：内部空间检查失败，包含非空气方块");
                return false;
            }

            // 确保内部空间足够大
            int innerWidth = width - 1;
            int innerHeight = height - 1;
            if (innerWidth < 2 || innerHeight < 3) {
                Blasphemy.LOGGER.info("传送门计算：内部空间太小, {}x{}", innerWidth, innerHeight);
                return false;
            }

            Blasphemy.LOGGER.info("传送门计算：内部空间检查通过, 大小={}x{}", innerWidth, innerHeight);
            return true;
        }

        /**
         * 传送门大小是否有效
         */
        public boolean isValid() {
            return foundPortal &&
                    width >= MIN_PORTAL_WIDTH &&
                    height >= MIN_PORTAL_HEIGHT &&
                    width <= MAX_PORTAL_WIDTH &&
                    height <= MAX_PORTAL_HEIGHT;
        }
    }

    /**
     * 创建传送门
     */
    public static void createPortal(World world, PortalFrameResult result) {
        if (world.isClient)
            return;

        Blasphemy.LOGGER.info("创建传送门：{}x{}，方向：{}，位置：{}",
                result.width, result.height, result.direction, result.bottomLeft);

        Direction.Axis axis = result.direction.getAxis();

        // 修正：从左下角往内一格开始，确保不会替换框架方块
        BlockPos start;
        if (axis == Direction.Axis.X) {
            // 东西方向的传送门
            start = result.bottomLeft.offset(Direction.EAST, 1).offset(Direction.UP, 1);
        } else {
            // 南北方向的传送门
            start = result.bottomLeft.offset(Direction.SOUTH, 1).offset(Direction.UP, 1);
        }

        int innerWidth = result.width - 2; // 内部宽度
        int innerHeight = result.height - 2; // 内部高度

        Blasphemy.LOGGER.info("传送门内部空间：{}x{}, 起始位置：{}", innerWidth, innerHeight, start);

        for (int y = 0; y < innerHeight; y++) {
            for (int x = 0; x < innerWidth; x++) {
                BlockPos portalPos;

                if (axis == Direction.Axis.X) {
                    // 东西方向的传送门
                    portalPos = start.offset(Direction.EAST, x).offset(Direction.UP, y);
                } else {
                    // 南北方向的传送门
                    portalPos = start.offset(Direction.SOUTH, x).offset(Direction.UP, y);
                }

                // 检查是否会覆盖框架方块
                if (result.frameBlocks.contains(portalPos)) {
                    Blasphemy.LOGGER.warn("尝试将传送门方块放置在框架位置：{}，已跳过", portalPos);
                    continue;
                }

                BlockState portalState = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis);
                world.setBlockState(portalPos, portalState, Block.NOTIFY_ALL);

                Blasphemy.LOGGER.info("放置传送门方块：{}", portalPos);
            }
        }

        Blasphemy.LOGGER.info("传送门创建完成");
    }

    /**
     * 传送门框架结果类
     * 存储验证成功的传送门框架信息
     */
    public static class PortalFrameResult {
        public int width; // 框架宽度
        public int height; // 框架高度
        public Direction direction; // 框架方向
        public BlockPos bottomLeft; // 左下角位置
        public Set<BlockPos> frameBlocks = new HashSet<>(); // 框架方块位置
    }
}