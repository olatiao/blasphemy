package com.blasphemy.portal

import com.blasphemy.Blasphemy
import com.blasphemy.config.ModConfig
import java.util.*
import kotlin.math.*
import net.minecraft.block.Blocks
import net.minecraft.block.NetherPortalBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 传送门框架验证器
 *
 * 支持内部空间从2x3到21x21的任意大小传送门 允许缺失最多4个角落 支持配置文件指定任意方块作为框架，可混用多种方块 支持配置文件指定任意点火物品
 */
class PortalFrameValidator {
    companion object {
        // 传送门大小限制
        const val MIN_PORTAL_WIDTH = 4 // 最小宽度（内部空间为2）
        const val MIN_PORTAL_HEIGHT = 5 // 最小高度（内部空间为3）
        const val MAX_PORTAL_SIZE = 21 // 最大内部空间大小

        // 框架验证参数
        const val MAX_MISSING_CORNERS = 4 // 允许缺失的最大角落数量

        // 搜索范围优化参数
        const val SMALL_FRAME_SEARCH_RADIUS = 10 // 小型框架搜索范围（内部空间<=8x8）
        const val MEDIUM_FRAME_SEARCH_RADIUS = 15 // 中型框架搜索范围（内部空间9x9-15x15）
        const val LARGE_FRAME_SEARCH_RADIUS = 25 // 大型框架搜索范围（内部空间>15x15）

        // 缓存已验证的框架结果
        private val portalCache = WeakHashMap<BlockPos, FrameValidationResult>()

        // 缓存时间戳，用于自动失效
        private val cacheTimestamps = WeakHashMap<BlockPos, Long>()

        // 缓存失效时间（毫秒）
        const val CACHE_EXPIRATION_TIME = 30000 // 30秒

        // 添加初始化方法
        @JvmStatic
        fun init() {
            Blasphemy.logger.info("传送门框架验证器初始化完成")
        }

        // 记录方块被破坏事件 - 在方块破坏监听器中调用
        @JvmStatic
        fun onBlockBreak(pos: BlockPos) {
            // 检查在缓存中的传送门是否受到影响
            val positionsToInvalidate = mutableSetOf<BlockPos>()

            // 找出所有可能受影响的缓存条目
            for ((cachedPos, result) in portalCache) {
                // 如果破坏的方块距离缓存位置很近，可能影响到这个框架
                if (isNearby(pos, cachedPos, SMALL_FRAME_SEARCH_RADIUS)) {
                    positionsToInvalidate.add(cachedPos)
                }
            }

            // 移除受影响的缓存条目
            for (invalidPos in positionsToInvalidate) {
                portalCache.remove(invalidPos)
                cacheTimestamps.remove(invalidPos)
                Blasphemy.logger.info("由于方块破坏，缓存位置 $invalidPos 的框架验证结果已失效")
            }
        }

        // 检查两个位置是否足够近
        private fun isNearby(pos1: BlockPos, pos2: BlockPos, radius: Int): Boolean {
            return pos1.isWithinDistance(pos2, radius.toDouble())
        }

        // 获取有效的框架方块列表
        @JvmStatic
        fun getValidFrameBlocks(): List<String> {
            return ModConfig.getConfig().portalConfig.portalBlocks
        }

        // 检查物品是否可以用于点亮传送门
        @JvmStatic
        fun isValidIgnitionItem(itemId: String): Boolean {
            val config = ModConfig.getConfig().portalConfig
            // 如果未指定点火物品，默认使用打火石
            return if (config.ignitionItem.isNotEmpty()) {
                config.ignitionItem == itemId
            } else {
                itemId == "minecraft:flint_and_steel"
            }
        }

        // 清除缓存
        @JvmStatic
        fun clearCache() {
            portalCache.clear()
            cacheTimestamps.clear()
            Blasphemy.logger.info("传送门验证缓存已清除")
        }
    }

    // 框架验证结果数据类
    data class FrameValidationResult(
            val frameBlocks: Set<BlockPos>, // 框架方块位置集合
            val direction: Direction, // 框架方向
            val width: Int, // 框架总宽度
            val height: Int, // 框架总高度
            val bottomLeft: BlockPos, // 左下角位置
            val innerWidth: Int, // 内部空间宽度
            val innerHeight: Int // 内部空间高度
    )

    /** 坐标轴处理器接口 处理不同方向上的坐标计算 */
    interface AxisHandler {
        fun getPos(bottomLeft: BlockPos, width: Int, height: Int): BlockPos
    }

    /** 针对北南朝向的坐标轴处理器 */
    class NorthSouthAxisHandler : AxisHandler {
        override fun getPos(bottomLeft: BlockPos, width: Int, height: Int): BlockPos {
            return BlockPos(bottomLeft.x + width, bottomLeft.y + height, bottomLeft.z)
        }
    }

    /** 针对东西朝向的坐标轴处理器 */
    class EastWestAxisHandler : AxisHandler {
        override fun getPos(bottomLeft: BlockPos, width: Int, height: Int): BlockPos {
            return BlockPos(bottomLeft.x, bottomLeft.y + height, bottomLeft.z + width)
        }
    }

    /** 获取坐标轴处理器 */
    private fun getAxisHandler(direction: Direction): AxisHandler {
        return when (direction.axis) {
            Direction.Axis.Z -> NorthSouthAxisHandler()
            Direction.Axis.X -> EastWestAxisHandler()
            else -> throw IllegalArgumentException("不支持的方向轴: ${direction.axis}")
        }
    }

    /** 验证传送门框架结构 - 主入口方法 */
    fun validateFrameStructure(world: World, startPos: BlockPos): FrameValidationResult? {
        val startTime = System.currentTimeMillis()
        Blasphemy.logger.info("开始验证传送门框架结构: ${startPos.x}, ${startPos.y}, ${startPos.z}")

        // 确保起始点是框架方块
        if (!isValidFrameBlock(world, startPos)) {
            Blasphemy.logger.info("起始点不是有效的框架方块")
            return null
        }

        // 检查缓存
        portalCache[startPos]?.let { cachedResult ->
            // 检查缓存是否过期
            val timestamp = cacheTimestamps[startPos] ?: 0L
            val currentTime = System.currentTimeMillis()
            val isExpired = currentTime - timestamp > CACHE_EXPIRATION_TIME

            if (isExpired) {
                Blasphemy.logger.info("缓存已过期，重新验证")
                portalCache.remove(startPos)
                cacheTimestamps.remove(startPos)
            }
            // 只有未过期的缓存才进行二次验证
            else if (verifyCachedFrameStillValid(world, cachedResult)) {
                Blasphemy.logger.info("从缓存中获取传送门框架结果")
                // 更新时间戳
                cacheTimestamps[startPos] = currentTime
                return cachedResult
            } else {
                // 如果框架已被破坏，移除缓存
                Blasphemy.logger.info("缓存的框架已被破坏，重新验证")
                portalCache.remove(startPos)
                cacheTimestamps.remove(startPos)
            }
        }

        // 确定优先检测方向
        val preferredAxis = detectPreferredAxis(world, startPos)
        Blasphemy.logger.info("优先检测轴方向: $preferredAxis")

        // 按优先顺序尝试不同的检测策略
        var result: FrameValidationResult? = null

        // 1. 先尝试检测小型框架（效率高，最常见的情况）
        result = detectSmallFrame(world, startPos, preferredAxis)
        if (result != null) {
            logSuccess(startTime, "小型")
            // 添加结果到缓存并记录时间戳
            portalCache[startPos] = result
            cacheTimestamps[startPos] = System.currentTimeMillis()
            return result
        }

        // 2. 再尝试检测中型框架
        result = detectMediumFrame(world, startPos, preferredAxis)
        if (result != null) {
            logSuccess(startTime, "中型")
            // 添加结果到缓存并记录时间戳
            portalCache[startPos] = result
            cacheTimestamps[startPos] = System.currentTimeMillis()
            return result
        }

        // 3. 最后尝试检测大型框架
        result = detectLargeFrame(world, startPos, preferredAxis)
        if (result != null) {
            logSuccess(startTime, "大型")
            // 添加结果到缓存并记录时间戳
            portalCache[startPos] = result
            cacheTimestamps[startPos] = System.currentTimeMillis()
            return result
        }

        Blasphemy.logger.info("未检测到有效的传送门框架")
        return null
    }

    /** 记录成功检测的日志和时间 */
    private fun logSuccess(startTime: Long, frameType: String) {
        val endTime = System.currentTimeMillis()
        Blasphemy.logger.info("${frameType}框架检测成功，耗时: ${endTime - startTime}ms")
    }

    /** 检测首选轴向 通过分析起始点周围的框架方块分布来确定最可能的框架方向 */
    private fun detectPreferredAxis(world: World, startPos: BlockPos): Direction.Axis {
        val xConnections = countDirectionalFrameBlocks(world, startPos, Direction.Axis.X)
        val zConnections = countDirectionalFrameBlocks(world, startPos, Direction.Axis.Z)

        return if (xConnections >= zConnections) Direction.Axis.X else Direction.Axis.Z
    }

    /** 统计指定轴向上的连接框架方块数量 */
    private fun countDirectionalFrameBlocks(
            world: World,
            pos: BlockPos,
            axis: Direction.Axis
    ): Int {
        var count = 0

        val directions =
                when (axis) {
                    Direction.Axis.X -> listOf(Direction.EAST, Direction.WEST)
                    Direction.Axis.Z -> listOf(Direction.NORTH, Direction.SOUTH)
                    else -> listOf()
                }

        for (dir in directions) {
            var current = pos
            for (i in 1..5) { // 检查5格范围内
                current = current.offset(dir)
                if (isValidFrameBlock(world, current)) {
                    count++
                } else {
                    break
                }
            }
        }

        return count
    }

    /** 检测小型框架 (内部空间 <= 8x8) */
    private fun detectSmallFrame(
            world: World,
            startPos: BlockPos,
            preferredAxis: Direction.Axis
    ): FrameValidationResult? {
        Blasphemy.logger.info("尝试检测小型框架...")

        // 收集周围的框架方块
        val frameBlocks = collectFrameBlocks(world, startPos, SMALL_FRAME_SEARCH_RADIUS)
        if (frameBlocks.size < 10) {
            return null // 框架方块太少，不太可能形成有效框架
        }

        // 优先检查的尺寸（最常见的小型框架）
        val commonSizes =
                listOf(
                        Pair(4, 5), // 内部2x3
                        Pair(4, 6), // 内部2x4
                        Pair(5, 5), // 内部3x3
                        Pair(6, 8), // 内部4x6
                        Pair(10, 10) // 内部8x8
                )

        // 尝试不同的方向
        val directions = getDirectionsByPriority(preferredAxis)

        for (direction in directions) {
            val result =
                    tryDetectFrameInDirection(world, startPos, frameBlocks, direction, commonSizes)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /** 检测中型框架 (内部空间 9x9-15x15) */
    private fun detectMediumFrame(
            world: World,
            startPos: BlockPos,
            preferredAxis: Direction.Axis
    ): FrameValidationResult? {
        Blasphemy.logger.info("尝试检测中型框架...")

        // 收集周围更大范围的框架方块
        val frameBlocks = collectFrameBlocks(world, startPos, MEDIUM_FRAME_SEARCH_RADIUS)
        if (frameBlocks.size < 30) {
            return null // 框架方块太少，不太可能形成中型框架
        }

        // 中型框架的尺寸范围
        val mediumSizes =
                listOf(
                        Pair(11, 11), // 内部9x9
                        Pair(13, 13), // 内部11x11
                        Pair(15, 15), // 内部13x13
                        Pair(17, 17) // 内部15x15
                )

        // 尝试不同的方向
        val directions = getDirectionsByPriority(preferredAxis)

        for (direction in directions) {
            val result =
                    tryDetectFrameInDirection(world, startPos, frameBlocks, direction, mediumSizes)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /** 检测大型框架 (内部空间 > 15x15) */
    private fun detectLargeFrame(
            world: World,
            startPos: BlockPos,
            preferredAxis: Direction.Axis
    ): FrameValidationResult? {
        Blasphemy.logger.info("尝试检测大型框架...")

        // 收集更大范围的框架方块
        val frameBlocks = collectFrameBlocks(world, startPos, LARGE_FRAME_SEARCH_RADIUS)
        if (frameBlocks.size < 60) {
            return null // 框架方块太少，不太可能形成大型框架
        }

        // 大型框架的尺寸范围
        val largeSizes =
                listOf(
                        Pair(19, 19), // 内部17x17
                        Pair(21, 21), // 内部19x19
                        Pair(23, 23) // 内部21x21
                )

        // 如果大型框架的方块数量不够多，尝试扩大搜索范围
        val expandedFrameBlocks =
                if (frameBlocks.size < 120) {
                    Blasphemy.logger.info("扩大搜索范围收集更多大型框架方块")
                    collectFrameBlocks(world, startPos, LARGE_FRAME_SEARCH_RADIUS + 10)
                } else {
                    frameBlocks
                }

        // 尝试不同的方向
        val directions = getDirectionsByPriority(preferredAxis)

        for (direction in directions) {
            val result =
                    tryDetectFrameInDirection(
                            world,
                            startPos,
                            expandedFrameBlocks,
                            direction,
                            largeSizes
                    )
            if (result != null) {
                return result
            }
        }

        return null
    }

    /** 根据优先轴向获取排序后的方向列表 */
    private fun getDirectionsByPriority(preferredAxis: Direction.Axis): List<Direction> {
        return Direction.values()
                .filter { it.axis != Direction.Axis.Y } // 只考虑水平方向
                .sortedBy { it.axis != preferredAxis } // 优先考虑首选轴向
    }

    /** 尝试在指定方向检测指定尺寸的框架 */
    private fun tryDetectFrameInDirection(
            world: World,
            startPos: BlockPos,
            frameBlocks: Set<BlockPos>,
            direction: Direction,
            potentialSizes: List<Pair<Int, Int>>
    ): FrameValidationResult? {
        val sideDirection = getSideDirection(direction)

        // 确定固定坐标值（z坐标对于南/北方向，x坐标对于东/西方向）
        val fixedValue =
                when (direction.axis) {
                    Direction.Axis.Z -> startPos.z
                    Direction.Axis.X -> startPos.x
                    else -> return null
                }

        // 找到同一平面上的所有框架方块
        val frameBlocksInPlane =
                frameBlocks.filter {
                    when (direction.axis) {
                        Direction.Axis.Z -> it.z == fixedValue
                        Direction.Axis.X -> it.x == fixedValue
                        else -> false
                    }
                }

        if (frameBlocksInPlane.isEmpty()) {
            return null
        }

        // 获取并分析坐标分布
        val coordinateData = analyzeCoordinates(frameBlocksInPlane, direction)
        if (!coordinateData.hasEnoughCoordinates()) {
            return null
        }

        // 尝试每种潜在的尺寸
        for ((width, height) in potentialSizes) {
            // 如果坐标范围不足以形成指定尺寸的框架，跳过
            if (coordinateData.yValues.size < height || coordinateData.hValues.size < width) {
                continue
            }

            // 寻找可能的框架位置
            val result =
                    findFrameOfSize(
                            world,
                            coordinateData,
                            direction,
                            fixedValue,
                            width,
                            height,
                            sideDirection
                    )
            if (result != null) {
                return result
            }
        }

        return null
    }

    /** 分析框架方块的坐标分布 */
    private data class CoordinateData(
            val yValues: List<Int>,
            val hValues: List<Int>,
            val direction: Direction
    ) {
        fun hasEnoughCoordinates(): Boolean {
            return yValues.size >= MIN_PORTAL_HEIGHT && hValues.size >= MIN_PORTAL_WIDTH
        }
    }

    private fun analyzeCoordinates(
            frameBlocks: Collection<BlockPos>,
            direction: Direction
    ): CoordinateData {
        // 收集Y坐标
        val yValues = frameBlocks.map { it.y }.distinct().sorted()

        // 收集水平坐标（根据方向轴）
        val hValues =
                frameBlocks
                        .map {
                            when (direction.axis) {
                                Direction.Axis.Z -> it.x // 南北方向使用X坐标
                                Direction.Axis.X -> it.z // 东西方向使用Z坐标
                                else -> 0
                            }
                        }
                        .distinct()
                        .sorted()

        return CoordinateData(yValues, hValues, direction)
    }

    /** 尝试找到指定尺寸的框架 - 优化版本 对大型框架使用更灵活的尺寸匹配和位置搜索 */
    private fun findFrameOfSize(
            world: World,
            coordinateData: CoordinateData,
            direction: Direction,
            fixedValue: Int,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): FrameValidationResult? {
        val yValues = coordinateData.yValues
        val hValues = coordinateData.hValues

        // 根据框架大小调整搜索策略
        val isLargeFrame = width > 15 || height > 15

        // 对于大型框架，允许更大的尺寸偏差
        val allowedDeviation = if (isLargeFrame) 3 else 1

        // 为大型框架增加尝试的起始位置数量
        val maxStartPositions = if (isLargeFrame) 15 else 10

        // 查找潜在的起始范围 - 对于大型框架，尝试更多的候选区域
        val yRanges = findContinuousRanges(yValues, height, maxStartPositions)
        val hRanges = findContinuousRanges(hValues, width, maxStartPositions)

        // 记录日志
        Blasphemy.logger.info(
                "寻找尺寸为${width}x${height}的框架，找到${yRanges.size}个Y轴范围和${hRanges.size}个水平轴范围"
        )

        // 首先尝试最接近目标尺寸的范围组合
        val sortedYRanges =
                yRanges.sortedBy { Math.abs((yValues[it.second] - yValues[it.first] + 1) - height) }
        val sortedHRanges =
                hRanges.sortedBy { Math.abs((hValues[it.second] - hValues[it.first] + 1) - width) }

        // 尝试每个可能的位置组合
        for ((yStart, yEnd) in sortedYRanges) {
            val actualHeight = yValues[yEnd] - yValues[yStart] + 1

            // 检查高度是否在可接受范围内（大型框架使用更宽松的匹配）
            if (actualHeight < height - allowedDeviation || actualHeight > height + allowedDeviation
            )
                    continue

            for ((hStart, hEnd) in sortedHRanges) {
                val actualWidth = hValues[hEnd] - hValues[hStart] + 1

                // 检查宽度是否在可接受范围内
                if (actualWidth < width - allowedDeviation || actualWidth > width + allowedDeviation
                )
                        continue

                // 构建框架的左下角位置
                val bottomLeft =
                        when (direction.axis) {
                            Direction.Axis.Z ->
                                    BlockPos(hValues[hStart], yValues[yStart], fixedValue)
                            Direction.Axis.X ->
                                    BlockPos(fixedValue, yValues[yStart], hValues[hStart])
                            else -> continue
                        }

                Blasphemy.logger.info(
                        "尝试验证框架: ${bottomLeft}, 尺寸=${actualWidth}x${actualHeight}, 目标=${width}x${height}"
                )

                // 检查框架的有效性
                if (isValidFrame(
                                world,
                                bottomLeft,
                                direction,
                                actualWidth,
                                actualHeight,
                                sideDirection
                        )
                ) {
                    // 检查内部空间
                    if (validateInnerSpace(
                                    world,
                                    bottomLeft,
                                    direction,
                                    actualWidth,
                                    actualHeight,
                                    sideDirection
                            )
                    ) {
                        // 收集框架方块
                        val frameBlocks =
                                collectFrameBlocks(
                                        world,
                                        bottomLeft,
                                        direction,
                                        actualWidth,
                                        actualHeight,
                                        sideDirection
                                )

                        Blasphemy.logger.info(
                                "找到有效框架! 尺寸=${actualWidth}x${actualHeight}, 内部=${actualWidth-2}x${actualHeight-2}"
                        )

                        // 创建结果对象
                        return FrameValidationResult(
                                frameBlocks = frameBlocks,
                                direction = direction,
                                width = actualWidth,
                                height = actualHeight,
                                bottomLeft = bottomLeft,
                                innerWidth = actualWidth - 2,
                                innerHeight = actualHeight - 2
                        )
                    }
                }
            }
        }

        // 如果没找到完全匹配的框架，尝试更灵活的尺寸匹配（仅限大型框架）
        if (isLargeFrame) {
            Blasphemy.logger.info("未找到精确匹配的大型框架，尝试更灵活的匹配...")

            // 对于大型框架，可以接受更小或更大的尺寸
            val alternativeSizes =
                    listOf(
                            Pair(width - 2, height - 2),
                            Pair(width - 1, height - 1),
                            Pair(width + 1, height + 1),
                            Pair(width + 2, height + 2)
                    )

            for ((altWidth, altHeight) in alternativeSizes) {
                if (altWidth < MIN_PORTAL_WIDTH || altHeight < MIN_PORTAL_HEIGHT) continue

                val result =
                        findAlternativeFrame(
                                world,
                                coordinateData,
                                direction,
                                fixedValue,
                                altWidth,
                                altHeight,
                                sideDirection
                        )
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /** 尝试查找替代尺寸的框架 用于大型框架的灵活匹配 */
    private fun findAlternativeFrame(
            world: World,
            coordinateData: CoordinateData,
            direction: Direction,
            fixedValue: Int,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): FrameValidationResult? {
        val yValues = coordinateData.yValues
        val hValues = coordinateData.hValues

        // 查找可能的范围
        val yRanges = findContinuousRanges(yValues, height, 5)
        val hRanges = findContinuousRanges(hValues, width, 5)

        // 尝试每个可能的位置组合
        for ((yStart, yEnd) in yRanges) {
            val actualHeight = yValues[yEnd] - yValues[yStart] + 1
            if (Math.abs(actualHeight - height) > 2) continue

            for ((hStart, hEnd) in hRanges) {
                val actualWidth = hValues[hEnd] - hValues[hStart] + 1
                if (Math.abs(actualWidth - width) > 2) continue

                // 构建框架的左下角位置
                val bottomLeft =
                        when (direction.axis) {
                            Direction.Axis.Z ->
                                    BlockPos(hValues[hStart], yValues[yStart], fixedValue)
                            Direction.Axis.X ->
                                    BlockPos(fixedValue, yValues[yStart], hValues[hStart])
                            else -> continue
                        }

                Blasphemy.logger.info("尝试替代尺寸框架: ${bottomLeft}, 尺寸=${actualWidth}x${actualHeight}")

                // 使用更宽松的验证标准
                if (isValidFrame(
                                world,
                                bottomLeft,
                                direction,
                                actualWidth,
                                actualHeight,
                                sideDirection
                        )
                ) {
                    if (validateInnerSpace(
                                    world,
                                    bottomLeft,
                                    direction,
                                    actualWidth,
                                    actualHeight,
                                    sideDirection
                            )
                    ) {
                        val frameBlocks =
                                collectFrameBlocks(
                                        world,
                                        bottomLeft,
                                        direction,
                                        actualWidth,
                                        actualHeight,
                                        sideDirection
                                )

                        Blasphemy.logger.info("找到替代尺寸有效框架! 尺寸=${actualWidth}x${actualHeight}")

                        return FrameValidationResult(
                                frameBlocks = frameBlocks,
                                direction = direction,
                                width = actualWidth,
                                height = actualHeight,
                                bottomLeft = bottomLeft,
                                innerWidth = actualWidth - 2,
                                innerHeight = actualHeight - 2
                        )
                    }
                }
            }
        }

        return null
    }

    /** 验证框架的有效性 */
    private fun isValidFrame(
            world: World,
            bottomLeft: BlockPos,
            direction: Direction,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): Boolean {
        // 检查框架是否超出尺寸限制
        if (width > MAX_PORTAL_SIZE + 2 || height > MAX_PORTAL_SIZE + 2) {
            Blasphemy.logger.info("框架尺寸${width}x${height}超出限制")
            return false
        }

        // 确保最小尺寸要求
        if (width < MIN_PORTAL_WIDTH || height < MIN_PORTAL_HEIGHT) {
            Blasphemy.logger.info("框架尺寸${width}x${height}小于最小限制")
            return false
        }

        // 检查角落（允许缺失部分）
        val missingCorners = checkCorners(world, bottomLeft, width, height, sideDirection)
        if (missingCorners > MAX_MISSING_CORNERS) {
            Blasphemy.logger.info("缺失角落数量${missingCorners}超过允许的最大数量${MAX_MISSING_CORNERS}")
            return false
        }

        // 添加额外验证：检查关键位置必须存在框架方块
        if (!validateKeyFramePositions(world, bottomLeft, width, height, sideDirection)) {
            Blasphemy.logger.info("关键框架位置验证失败")
            return false
        }

        // 验证边缘的完整性
        return verifyFrameEdges(
                world,
                bottomLeft,
                direction,
                width,
                height,
                sideDirection,
                missingCorners
        )
    }

    /**
     * 检查框架的角落
     * @return 缺失的角落数量
     */
    private fun checkCorners(
            world: World,
            bottomLeft: BlockPos,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): Int {
        val corners =
                listOf(
                        bottomLeft, // 左下
                        bottomLeft.offset(sideDirection, width - 1), // 右下
                        bottomLeft.offset(Direction.UP, height - 1), // 左上
                        bottomLeft
                                .offset(sideDirection, width - 1)
                                .offset(Direction.UP, height - 1) // 右上
                )

        return corners.count { !isValidFrameBlock(world, it) }
    }

    /** 验证框架边缘的完整性 允许部分边缘方块缺失，但必须保持整体结构 */
    private fun verifyFrameEdges(
            world: World,
            bottomLeft: BlockPos,
            direction: Direction,
            width: Int,
            height: Int,
            sideDirection: Direction,
            missingCorners: Int
    ): Boolean {
        // 确定验证参数（根据框架大小调整）
        val (requiredPercentage, segmentSize) = getFrameSizeParameters(width, height)

        // 验证底部边缘
        if (!verifyEdge(
                        world,
                        bottomLeft.offset(sideDirection, 1),
                        sideDirection,
                        width - 2,
                        segmentSize,
                        requiredPercentage
                )
        ) {
            return false
        }

        // 验证顶部边缘
        if (!verifyEdge(
                        world,
                        bottomLeft.offset(Direction.UP, height - 1).offset(sideDirection, 1),
                        sideDirection,
                        width - 2,
                        segmentSize,
                        requiredPercentage
                )
        ) {
            return false
        }

        // 验证左侧边缘
        if (!verifyEdge(
                        world,
                        bottomLeft.offset(Direction.UP, 1),
                        Direction.UP,
                        height - 2,
                        segmentSize,
                        requiredPercentage
                )
        ) {
            return false
        }

        // 验证右侧边缘
        if (!verifyEdge(
                        world,
                        bottomLeft.offset(sideDirection, width - 1).offset(Direction.UP, 1),
                        Direction.UP,
                        height - 2,
                        segmentSize,
                        requiredPercentage
                )
        ) {
            return false
        }

        return true
    }

    /** 根据框架大小获取验证参数 较大的框架使用更宽松的验证标准 */
    private fun getFrameSizeParameters(width: Int, height: Int): Pair<Double, Int> {
        // 根据框架大小调整参数，调整为更严格的完整度要求
        return when {
            width > 20 || height > 20 -> Pair(85.0, 10) // 超大框架：较严格的标准
            width > 15 || height > 15 -> Pair(90.0, 8) // 大框架：严格的标准
            width > 10 || height > 10 -> Pair(95.0, 6) // 中型框架：很严格的标准
            else -> Pair(98.0, 4) // 小型框架：极严格的标准
        }
    }

    /** 验证单个边缘 - 优化版本，支持更灵活的边缘检测 */
    private fun verifyEdge(
            world: World,
            start: BlockPos,
            direction: Direction,
            length: Int,
            segmentSize: Int,
            requiredPercentage: Double
    ): Boolean {
        if (length <= 0) return true

        // 对于极短的边缘，要求至少75%的方块存在
        if (length <= 4) {
            var validBlocks = 0
            for (i in 0 until length) {
                val pos = start.offset(direction, i)
                if (isValidFrameBlock(world, pos)) {
                    validBlocks++
                }
            }
            return validBlocks >= ceil(length * 0.75)
        }

        // 对于较长的边缘，使用分段验证
        val segments = max(1, ceil(length.toDouble() / segmentSize).toInt())
        val segmentLength = length.toDouble() / segments

        var totalValidBlocks = 0
        var validSegments = 0
        var consecutiveInvalidBlocks = 0
        var maxConsecutiveInvalidBlocks = 0

        for (segIdx in 0 until segments) {
            val segStart = floor(segIdx * segmentLength).toInt()
            val segEnd = floor((segIdx + 1) * segmentLength).toInt()
            val segSize = segEnd - segStart

            if (segSize <= 0) continue

            var segValidBlocks = 0
            consecutiveInvalidBlocks = 0

            for (i in 0 until segSize) {
                val pos = start.offset(direction, segStart + i)
                if (isValidFrameBlock(world, pos)) {
                    segValidBlocks++
                    totalValidBlocks++
                    consecutiveInvalidBlocks = 0
                } else {
                    consecutiveInvalidBlocks++
                    maxConsecutiveInvalidBlocks =
                            max(maxConsecutiveInvalidBlocks, consecutiveInvalidBlocks)
                }
            }

            // 段完整度
            val segPercentage = (segValidBlocks * 100.0) / segSize
            if (segPercentage >= requiredPercentage) {
                validSegments++
            }
        }

        // 整体完整度
        val overallPercentage = (totalValidBlocks * 100.0) / length

        // 检测大缺口 - 如果连续缺失超过一定数量，认为框架不完整
        val maxAllowedConsecutiveGap =
                when {
                    length > 20 -> 2 // 超大框架最多允许连续缺失2个
                    length > 10 -> 1 // 大中型框架最多允许连续缺失1个
                    else -> 0 // 小型框架不允许有缺失
                }

        if (maxConsecutiveInvalidBlocks > maxAllowedConsecutiveGap) {
            Blasphemy.logger.info(
                    "边缘验证失败：存在连续${maxConsecutiveInvalidBlocks}个缺失方块，超过允许的${maxAllowedConsecutiveGap}个"
            )
            return false
        }

        // 使用两个标准同时满足：
        // 1. 足够多的段达到要求
        // 2. 整体完整度达到要求
        // 修改为AND逻辑，增加严格性
        val result =
                (validSegments >= segments * 0.7) &&
                        (overallPercentage >= requiredPercentage * 0.85)

        if (!result) {
            Blasphemy.logger.info(
                    "边缘验证失败：有效段比例=${validSegments.toDouble()/segments}，整体完整度=${overallPercentage}%"
            )
        }

        return result
    }

    /** 验证内部空间是否没有障碍物 */
    private fun validateInnerSpace(
            world: World,
            bottomLeft: BlockPos,
            direction: Direction,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): Boolean {
        // 检查内部每个位置是否为空气
        for (h in 1 until height - 1) {
            for (w in 1 until width - 1) {
                val pos = bottomLeft.offset(sideDirection, w).offset(Direction.UP, h)
                if (!world.getBlockState(pos).isAir) {
                    return false
                }
            }
        }
        return true
    }

    /** 获取侧向方向 */
    private fun getSideDirection(direction: Direction): Direction {
        return when (direction) {
            Direction.NORTH -> Direction.EAST
            Direction.SOUTH -> Direction.WEST
            Direction.EAST -> Direction.SOUTH
            Direction.WEST -> Direction.NORTH
            else -> Direction.EAST // 默认
        }
    }

    /** 收集框架方块 针对大型框架进行优化，使用更高效的方块收集算法 */
    private fun collectFrameBlocks(
            world: World,
            bottomLeft: BlockPos,
            direction: Direction,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): Set<BlockPos> {
        // 重用坐标系辅助对象
        val axisHandler = getAxisHandler(direction)

        // 创建存储所有框架方块的集合
        val frameBlocks = mutableSetOf<BlockPos>()

        // 根据框架大小确定是否需要更高效的收集算法
        val isLargeFrame = width > 15 || height > 15

        if (isLargeFrame) {
            Blasphemy.logger.info("正在为大型框架(${width}x${height})优化方块收集...")

            // 对于非常大的框架，使用间隔采样以加快收集速度
            val samplingInterval =
                    when {
                        width > 30 || height > 30 -> 3 // 超大型框架使用间隔3的采样
                        width > 20 || height > 20 -> 2 // 大型框架使用间隔2的采样
                        else -> 1 // 其他框架使用连续采样
                    }

            // 收集底部边缘
            collectEdge(frameBlocks, axisHandler, bottomLeft, width, 0, true, samplingInterval)

            // 收集顶部边缘
            collectEdge(
                    frameBlocks,
                    axisHandler,
                    bottomLeft,
                    width,
                    height - 1,
                    true,
                    samplingInterval
            )

            // 收集左侧边缘
            collectEdge(frameBlocks, axisHandler, bottomLeft, height, 0, false, samplingInterval)

            // 收集右侧边缘
            collectEdge(
                    frameBlocks,
                    axisHandler,
                    bottomLeft,
                    height,
                    width - 1,
                    false,
                    samplingInterval
            )

            // 对于角点，确保一定收集到
            frameBlocks.add(axisHandler.getPos(bottomLeft, 0, 0)) // 左下角
            frameBlocks.add(axisHandler.getPos(bottomLeft, width - 1, 0)) // 右下角
            frameBlocks.add(axisHandler.getPos(bottomLeft, 0, height - 1)) // 左上角
            frameBlocks.add(axisHandler.getPos(bottomLeft, width - 1, height - 1)) // 右上角
        } else {
            // 对于小型框架，使用原始的完整收集方式

            // 收集底部边缘
            for (w in 0 until width) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, w, 0))
            }

            // 收集顶部边缘
            for (w in 0 until width) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, w, height - 1))
            }

            // 收集左侧边缘
            for (h in 1 until height - 1) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, 0, h))
            }

            // 收集右侧边缘
            for (h in 1 until height - 1) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, width - 1, h))
            }
        }

        Blasphemy.logger.info("收集了${frameBlocks.size}个框架方块")
        return frameBlocks
    }

    /** 收集一条边缘上的方块 支持间隔采样以提高大型框架的效率 */
    private fun collectEdge(
            frameBlocks: MutableSet<BlockPos>,
            axisHandler: AxisHandler,
            bottomLeft: BlockPos,
            length: Int,
            fixedCoord: Int,
            isHorizontal: Boolean,
            samplingInterval: Int
    ) {
        // 根据是水平还是垂直边缘决定坐标轴
        if (isHorizontal) {
            // 水平边缘 (底部/顶部)
            for (w in 0 until length step samplingInterval) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, w, fixedCoord))

                // 确保最后一个方块被收集到
                val nextW = w + samplingInterval
                if (nextW >= length && w != length - 1) {
                    frameBlocks.add(axisHandler.getPos(bottomLeft, length - 1, fixedCoord))
                }
            }
        } else {
            // 垂直边缘 (左侧/右侧)
            for (h in 1 until length - 1 step samplingInterval) {
                frameBlocks.add(axisHandler.getPos(bottomLeft, fixedCoord, h))

                // 确保最后一个方块被收集到
                val nextH = h + samplingInterval
                if (nextH >= length - 1 && h != length - 2) {
                    frameBlocks.add(axisHandler.getPos(bottomLeft, fixedCoord, length - 2))
                }
            }
        }
    }

    /** 收集指定半径内的框架方块 - 用于初始扫描 */
    private fun collectFrameBlocks(world: World, startPos: BlockPos, radius: Int): Set<BlockPos> {
        val frameBlocks = mutableSetOf<BlockPos>()
        val maxBlocks = 2000 // 增加最大收集数量，以支持大型框架

        // 使用简单的球形搜索
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    // 计算距离
                    val distSq = x * x + y * y + z * z
                    if (distSq <= radius * radius) {
                        val pos = startPos.add(x, y, z)
                        if (isValidFrameBlock(world, pos)) {
                            frameBlocks.add(pos)

                            // 如果已经收集足够的方块，提前返回
                            if (frameBlocks.size >= maxBlocks) {
                                return frameBlocks
                            }
                        }
                    }
                }
            }
        }

        return frameBlocks
    }

    /** 判断方块是否是有效的框架方块 */
    fun isValidFrameBlock(world: World, pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        val blockId = Registries.BLOCK.getId(block).toString()
        return getValidFrameBlocks().contains(blockId)
    }

    /** 用物品点亮传送门 支持消耗耐久或物品 */
    fun ignitePortal(world: World, result: FrameValidationResult, itemStack: ItemStack): Boolean {
        val direction = result.direction
        val width = result.width
        val height = result.height
        val bottomLeft = result.bottomLeft
        val sideDirection = getSideDirection(direction)

        // 确定传送门方向轴
        val axis =
                when (direction.axis) {
                    Direction.Axis.Z -> Direction.Axis.X // 南北朝向用X轴
                    Direction.Axis.X -> Direction.Axis.Z // 东西朝向用Z轴
                    else -> Direction.Axis.X // 默认
                }

        // 在框架内部创建传送门方块
        for (h in 1 until height - 1) {
            for (w in 1 until width - 1) {
                val portalPos = bottomLeft.offset(sideDirection, w).offset(Direction.UP, h)
                val portalState =
                        Blocks.NETHER_PORTAL.defaultState.with(NetherPortalBlock.AXIS, axis)
                world.setBlockState(portalPos, portalState)
            }
        }

        // 消耗物品耐久或物品本身
        if (itemStack.isDamageable) {
            itemStack.damage(1, world.random, null)
        } else {
            itemStack.decrement(1)
        }

        Blasphemy.logger.info("传送门点亮成功，内部尺寸: ${result.innerWidth}x${result.innerHeight}")
        return true
    }

    /** 创建传送门方块（兼容性方法） */
    fun createPortal(world: World, result: FrameValidationResult) {
        val direction = result.direction
        val width = result.width
        val height = result.height
        val bottomLeft = result.bottomLeft
        val sideDirection = getSideDirection(direction)

        // 确定传送门方向轴
        val axis =
                when (direction.axis) {
                    Direction.Axis.Z -> Direction.Axis.X // 南北朝向用X轴
                    Direction.Axis.X -> Direction.Axis.Z // 东西朝向用Z轴
                    else -> Direction.Axis.X // 默认
                }

        // 在框架内部创建传送门方块
        for (h in 1 until height - 1) {
            for (w in 1 until width - 1) {
                val portalPos = bottomLeft.offset(sideDirection, w).offset(Direction.UP, h)
                val portalState =
                        Blocks.NETHER_PORTAL.defaultState.with(NetherPortalBlock.AXIS, axis)
                world.setBlockState(portalPos, portalState)
            }
        }

        Blasphemy.logger.info("传送门创建完成，内部尺寸：${result.innerWidth}x${result.innerHeight}")
    }

    /** 向玩家显示调试信息 */
    fun showDebugInfo(player: PlayerEntity, result: FrameValidationResult) {
        player.sendMessage(Text.literal("======= 传送门框架信息 ======="), false)
        player.sendMessage(Text.literal("§b方向: §f${result.direction}"), false)
        player.sendMessage(Text.literal("§b框架尺寸: §f${result.width}x${result.height}"), false)
        player.sendMessage(
                Text.literal("§b内部空间: §f${result.innerWidth}x${result.innerHeight}"),
                false
        )
        player.sendMessage(Text.literal("§b框架方块数量: §f${result.frameBlocks.size}"), false)
        player.sendMessage(Text.literal("==========================="), false)
    }

    /** 查找连续的坐标范围 */
    private fun findContinuousRanges(
            values: List<Int>,
            targetSize: Int,
            maxRanges: Int
    ): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()

        // 直接尝试固定大小的窗口
        for (start in 0..values.size - targetSize) {
            val end = start + targetSize - 1
            ranges.add(Pair(start, end))

            // 限制范围数量以提高性能
            if (ranges.size >= maxRanges) break
        }

        // 如果没有找到足够的范围，尝试查找连续区域
        if (ranges.size < maxRanges && values.size >= 2) {
            var start = 0
            for (i in 1 until values.size) {
                if (values[i] - values[i - 1] > 1) {
                    // 如果区域足够大，加入候选
                    if (i - start >= targetSize) {
                        // 避免重复添加
                        val newRange = Pair(start, i - 1)
                        if (!ranges.contains(newRange)) {
                            ranges.add(newRange)
                        }
                    }
                    start = i
                }
            }

            // 处理最后一个区域
            if (values.size - start >= targetSize) {
                val newRange = Pair(start, values.size - 1)
                if (!ranges.contains(newRange)) {
                    ranges.add(newRange)
                }
            }
        }

        return ranges
    }

    /** 验证关键框架位置，确保框架基本完整 */
    private fun validateKeyFramePositions(
            world: World,
            bottomLeft: BlockPos,
            width: Int,
            height: Int,
            sideDirection: Direction
    ): Boolean {
        // 检查每条边的中间位置必须有框架方块

        // 底部中间位置
        val bottomMid = bottomLeft.offset(sideDirection, width / 2)
        if (!isValidFrameBlock(world, bottomMid)) {
            return false
        }

        // 顶部中间位置
        val topMid = bottomLeft.offset(Direction.UP, height - 1).offset(sideDirection, width / 2)
        if (!isValidFrameBlock(world, topMid)) {
            return false
        }

        // 左侧中间位置
        val leftMid = bottomLeft.offset(Direction.UP, height / 2)
        if (!isValidFrameBlock(world, leftMid)) {
            return false
        }

        // 右侧中间位置
        val rightMid = bottomLeft.offset(sideDirection, width - 1).offset(Direction.UP, height / 2)
        if (!isValidFrameBlock(world, rightMid)) {
            return false
        }

        return true
    }

    /** 验证缓存的框架是否仍然有效 */
    private fun verifyCachedFrameStillValid(
            world: World,
            frameResult: FrameValidationResult
    ): Boolean {
        // 检查框架的关键位置
        val bottomLeft = frameResult.bottomLeft
        val width = frameResult.width
        val height = frameResult.height
        val direction = frameResult.direction
        val sideDirection = getSideDirection(direction)

        // 检查四个角落
        val corners =
                listOf(
                        bottomLeft, // 左下
                        bottomLeft.offset(sideDirection, width - 1), // 右下
                        bottomLeft.offset(Direction.UP, height - 1), // 左上
                        bottomLeft
                                .offset(sideDirection, width - 1)
                                .offset(Direction.UP, height - 1) // 右上
                )

        // 如果有超过MAX_MISSING_CORNERS个角落缺失，框架无效
        val missingCorners = corners.count { !isValidFrameBlock(world, it) }
        if (missingCorners > MAX_MISSING_CORNERS) {
            Blasphemy.logger.info("缓存框架验证：角落缺失过多(${missingCorners})")
            return false
        }

        // 检查中点位置
        if (!validateKeyFramePositions(world, bottomLeft, width, height, sideDirection)) {
            Blasphemy.logger.info("缓存框架验证：关键位置缺失")
            return false
        }

        // 抽样检查边缘上的一些点
        val edgePoints = getEdgeSamplePoints(bottomLeft, width, height, direction, sideDirection)
        val missingEdgePoints = edgePoints.count { !isValidFrameBlock(world, it) }
        val maxAllowedMissingPoints =
                when {
                    width > 15 || height > 15 -> 3 // 大型框架允许更多缺失
                    width > 10 || height > 10 -> 2 // 中型框架
                    else -> 1 // 小型框架
                }

        if (missingEdgePoints > maxAllowedMissingPoints) {
            Blasphemy.logger.info("缓存框架验证：边缘缺失过多(${missingEdgePoints}/${edgePoints.size})")
            return false
        }

        // 检查内部空间是否仍然为空
        val axisHandler = getAxisHandler(direction)
        for (h in 1 until height - 1) {
            for (w in 1 until width - 1) {
                val pos = axisHandler.getPos(bottomLeft, w, h)
                val block = world.getBlockState(pos).block
                // 检查是否为空气或者传送门方块
                if (!world.getBlockState(pos).isAir && block != Blocks.NETHER_PORTAL) {
                    Blasphemy.logger.info("缓存框架验证：内部空间有障碍物")
                    return false
                }
            }
        }

        return true
    }

    /** 获取边缘上的采样点进行快速验证 */
    private fun getEdgeSamplePoints(
            bottomLeft: BlockPos,
            width: Int,
            height: Int,
            direction: Direction,
            sideDirection: Direction
    ): List<BlockPos> {
        val samplePoints = mutableListOf<BlockPos>()
        val axisHandler = getAxisHandler(direction)

        // 采样数量根据框架大小调整
        val sampleCount =
                when {
                    width > 15 || height > 15 -> 8 // 大型框架采样更多点
                    width > 10 || height > 10 -> 6 // 中型框架
                    else -> 4 // 小型框架
                }

        // 底部边缘采样
        for (i in 0 until sampleCount) {
            val w = (i * width / sampleCount).coerceIn(0, width - 1)
            samplePoints.add(axisHandler.getPos(bottomLeft, w, 0))
        }

        // 顶部边缘采样
        for (i in 0 until sampleCount) {
            val w = (i * width / sampleCount).coerceIn(0, width - 1)
            samplePoints.add(axisHandler.getPos(bottomLeft, w, height - 1))
        }

        // 左侧边缘采样
        for (i in 1 until sampleCount - 1) {
            val h = (i * height / sampleCount).coerceIn(1, height - 2)
            samplePoints.add(axisHandler.getPos(bottomLeft, 0, h))
        }

        // 右侧边缘采样
        for (i in 1 until sampleCount - 1) {
            val h = (i * height / sampleCount).coerceIn(1, height - 2)
            samplePoints.add(axisHandler.getPos(bottomLeft, width - 1, h))
        }

        return samplePoints
    }
}
