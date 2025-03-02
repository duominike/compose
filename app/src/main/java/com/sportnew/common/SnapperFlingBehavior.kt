package com.sportnew.common

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@RequiresOptIn(message = "Snapper is experimental. The API may be changed in the future.")
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalSnapperApi

@ExperimentalSnapperApi
@Composable
fun rememberSnapperFlingBehavior(
    lazyListState: LazyListState,
    snapOffsetForItem: (layoutInfo: SnapperLayoutInfo, item: SnapperLayoutItemInfo) -> Int = SnapOffsets.Center,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
    springAnimationSpec: AnimationSpec<Float> = SnapperFlingBehaviorDefaults.SpringAnimationSpec,
    snapIndex: (SnapperLayoutInfo, startIndex: Int, targetIndex: Int) -> Int = SnapperFlingBehaviorDefaults.SnapIndex,
): SnapperFlingBehavior = rememberSnapperFlingBehavior(
    layoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState, snapOffsetForItem),
    decayAnimationSpec = decayAnimationSpec,
    springAnimationSpec = springAnimationSpec,
    snapIndex = snapIndex,
)

@ExperimentalSnapperApi
@Composable
fun rememberLazyListSnapperLayoutInfo(
    lazyListState: LazyListState,
    snapOffsetForItem: (layoutInfo: SnapperLayoutInfo, item: SnapperLayoutItemInfo) -> Int = SnapOffsets.Center,
): LazyListSnapperLayoutInfo = remember(lazyListState, snapOffsetForItem) {
    LazyListSnapperLayoutInfo(
        lazyListState = lazyListState,
        snapOffsetForItem = snapOffsetForItem,
    )
}

@ExperimentalSnapperApi
class LazyListSnapperLayoutInfo(
    private val lazyListState: LazyListState,
    private val snapOffsetForItem: (layoutInfo: SnapperLayoutInfo, item: SnapperLayoutItemInfo) -> Int,
) : SnapperLayoutInfo() {

    override val startScrollOffset: Int = 0

    override val endScrollOffset: Int
        get() = lazyListState.layoutInfo.let { it.viewportEndOffset - it.afterContentPadding }

    private val itemCount: Int get() = lazyListState.layoutInfo.totalItemsCount

    override val totalItemsCount: Int
        get() = lazyListState.layoutInfo.totalItemsCount

    override val currentItem: SnapperLayoutItemInfo? by derivedStateOf {
        visibleItems.lastOrNull { it.offset <= snapOffsetForItem(this, it) }
    }

    override val visibleItems: Sequence<SnapperLayoutItemInfo>
        get() = lazyListState.layoutInfo.visibleItemsInfo.asSequence()
            .map(::LazyListSnapperLayoutItemInfo)

    override fun distanceToIndexSnap(index: Int): Int {
        val itemInfo = visibleItems.firstOrNull { it.index == index }
        if (itemInfo != null) {
            return itemInfo.offset - snapOffsetForItem(this, itemInfo)
        }

        val currentItem = currentItem ?: return 0
        return ((index - currentItem.index) * estimateDistancePerItem()).roundToInt() +
                currentItem.offset -
                snapOffsetForItem(this, currentItem)
    }

    override fun canScrollTowardsStart(): Boolean {
        return lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
            it.index > 0 || it.offset < startScrollOffset
        } ?: false
    }

    override fun canScrollTowardsEnd(): Boolean {
        return lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.let {
            it.index < itemCount - 1 || (it.offset + it.size) > endScrollOffset
        } ?: false
    }

    override fun determineTargetIndex(
        velocity: Float,
        decayAnimationSpec: DecayAnimationSpec<Float>,
        maximumFlingDistance: Float,
    ): Int {
        val curr = currentItem ?: return -1

        val distancePerItem = estimateDistancePerItem()
        if (distancePerItem <= 0) {
            return curr.index
        }

        val distanceToCurrent = distanceToIndexSnap(curr.index)
        val distanceToNext = distanceToIndexSnap(curr.index + 1)

        if (abs(velocity) < 0.5f) {
            return when {
                distanceToCurrent.absoluteValue < distanceToNext.absoluteValue -> curr.index
                else -> curr.index + 1
            }.coerceIn(0, itemCount - 1)
        }

        val flingDistance = decayAnimationSpec.calculateTargetValue(0f, velocity)
            .coerceIn(-maximumFlingDistance, maximumFlingDistance)
            .let { distance ->
                if (velocity < 0) {
                    (distance + distanceToNext).coerceAtMost(0f)
                } else {
                    (distance + distanceToCurrent).coerceAtLeast(0f)
                }
            }

        val flingIndexDelta = flingDistance / distancePerItem.toDouble()
        val currentItemOffsetRatio = distanceToCurrent / distancePerItem.toDouble()

        val indexOffset = (flingIndexDelta - currentItemOffsetRatio).roundToInt()

        return (curr.index + indexOffset).coerceIn(0, itemCount - 1)
    }

    private fun calculateItemSpacing(): Int = with(lazyListState.layoutInfo) {
        if (visibleItemsInfo.size >= 2) {
            val first = visibleItemsInfo[0]
            val second = visibleItemsInfo[1]
            second.offset - (first.size + first.offset)
        } else 0
    }

    private fun estimateDistancePerItem(): Float = with(lazyListState.layoutInfo) {
        if (visibleItemsInfo.isEmpty()) return -1f

        val minPosView = visibleItemsInfo.minByOrNull { it.offset } ?: return -1f
        val maxPosView = visibleItemsInfo.maxByOrNull { it.offset + it.size } ?: return -1f

        val start = min(minPosView.offset, maxPosView.offset)
        val end = max(minPosView.offset + minPosView.size, maxPosView.offset + maxPosView.size)

        return when (val distance = end - start) {
            0 -> -1f
            else -> (distance + calculateItemSpacing()) / visibleItemsInfo.size.toFloat()
        }
    }
}


private class LazyListSnapperLayoutItemInfo(
    private val lazyListItem: LazyListItemInfo,
) : SnapperLayoutItemInfo() {
    override val index: Int get() = lazyListItem.index
    override val offset: Int get() = lazyListItem.offset
    override val size: Int get() = lazyListItem.size
}

@ExperimentalSnapperApi
object SnapperFlingBehaviorDefaults {
    val SpringAnimationSpec: AnimationSpec<Float> = spring(stiffness = 400f)

    val MaximumFlingDistance: (SnapperLayoutInfo) -> Float = { Float.MAX_VALUE }

    val SnapIndex: (SnapperLayoutInfo, startIndex: Int, targetIndex: Int) -> Int =
        { _, _, targetIndex -> targetIndex }
}

@ExperimentalSnapperApi
@Composable
fun rememberSnapperFlingBehavior(
    layoutInfo: SnapperLayoutInfo,
    decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
    springAnimationSpec: AnimationSpec<Float> = SnapperFlingBehaviorDefaults.SpringAnimationSpec,
    snapIndex: (SnapperLayoutInfo, startIndex: Int, targetIndex: Int) -> Int,
): SnapperFlingBehavior = remember(
    layoutInfo,
    decayAnimationSpec,
    springAnimationSpec,
    snapIndex,
) {
    SnapperFlingBehavior(
        layoutInfo = layoutInfo,
        decayAnimationSpec = decayAnimationSpec,
        springAnimationSpec = springAnimationSpec,
        snapIndex = snapIndex,
    )
}

@ExperimentalSnapperApi
abstract class SnapperLayoutInfo {
    abstract val startScrollOffset: Int

    abstract val endScrollOffset: Int

    abstract val visibleItems: Sequence<SnapperLayoutItemInfo>

    abstract val currentItem: SnapperLayoutItemInfo?

    abstract val totalItemsCount: Int

    abstract fun determineTargetIndex(
        velocity: Float,
        decayAnimationSpec: DecayAnimationSpec<Float>,
        maximumFlingDistance: Float,
    ): Int

    abstract fun distanceToIndexSnap(index: Int): Int

    abstract fun canScrollTowardsStart(): Boolean

    abstract fun canScrollTowardsEnd(): Boolean
}

abstract class SnapperLayoutItemInfo {
    abstract val index: Int
    abstract val offset: Int
    abstract val size: Int

    override fun toString(): String {
        return "SnapperLayoutItemInfo(index=$index, offset=$offset, size=$size)"
    }
}

@ExperimentalSnapperApi
@Suppress("unused")
object SnapOffsets {
    val Start: (SnapperLayoutInfo, SnapperLayoutItemInfo) -> Int =
        { layout, _ -> layout.startScrollOffset }

    val Center: (SnapperLayoutInfo, SnapperLayoutItemInfo) -> Int = { layout, item ->
        layout.startScrollOffset + (layout.endScrollOffset - layout.startScrollOffset - item.size) / 2
    }

    val End: (SnapperLayoutInfo, SnapperLayoutItemInfo) -> Int = { layout, item ->
        layout.endScrollOffset - item.size
    }
}

@ExperimentalSnapperApi
class SnapperFlingBehavior private constructor(
    private val layoutInfo: SnapperLayoutInfo,
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
    private val springAnimationSpec: AnimationSpec<Float>,
    private val snapIndex: (SnapperLayoutInfo, startIndex: Int, targetIndex: Int) -> Int,
    private val maximumFlingDistance: (SnapperLayoutInfo) -> Float,
) : FlingBehavior {
    constructor(
        layoutInfo: SnapperLayoutInfo,
        decayAnimationSpec: DecayAnimationSpec<Float>,
        springAnimationSpec: AnimationSpec<Float> = SnapperFlingBehaviorDefaults.SpringAnimationSpec,
        snapIndex: (SnapperLayoutInfo, startIndex: Int, targetIndex: Int) -> Int = SnapperFlingBehaviorDefaults.SnapIndex,
    ) : this(
        layoutInfo = layoutInfo,
        decayAnimationSpec = decayAnimationSpec,
        springAnimationSpec = springAnimationSpec,
        snapIndex = snapIndex,
        maximumFlingDistance = (SnapperFlingBehaviorDefaults.MaximumFlingDistance),
    )

    private var animationTarget: Int? by mutableStateOf(null)

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float
    ): Float {
        if (!layoutInfo.canScrollTowardsStart() || !layoutInfo.canScrollTowardsEnd()) {
            return initialVelocity
        }

        val maxFlingDistance = maximumFlingDistance(layoutInfo)
        require(maxFlingDistance > 0) {
            "Distance returned by maximumFlingDistance should be greater than 0"
        }

        val initialItem = layoutInfo.currentItem ?: return initialVelocity

        val targetIndex = layoutInfo.determineTargetIndex(
            velocity = initialVelocity,
            decayAnimationSpec = decayAnimationSpec,
            maximumFlingDistance = maxFlingDistance,
        ).let { target ->
            snapIndex(
                layoutInfo,
                if (initialVelocity < 0) initialItem.index + 1 else initialItem.index,
                target,
            )
        }.also {
            require(it in 0 until layoutInfo.totalItemsCount)
        }

        return flingToIndex(index = targetIndex, initialVelocity = initialVelocity)
    }

    private suspend fun ScrollScope.flingToIndex(
        index: Int,
        initialVelocity: Float,
    ): Float {
        val initialItem = layoutInfo.currentItem ?: return initialVelocity

        if (initialItem.index == index && layoutInfo.distanceToIndexSnap(initialItem.index) == 0) {
            return consumeVelocityIfNotAtScrollEdge(initialVelocity)
        }

        var velocityLeft = initialVelocity

        if (decayAnimationSpec.canDecayBeyondCurrentItem(initialVelocity, initialItem)) {
            velocityLeft = performDecayFling(
                initialItem = initialItem,
                targetIndex = index,
                initialVelocity = velocityLeft,
            )
        }

        val currentItem = layoutInfo.currentItem ?: return initialVelocity
        if (currentItem.index != index || layoutInfo.distanceToIndexSnap(index) != 0) {
            velocityLeft = performSpringFling(
                initialItem = currentItem,
                targetIndex = index,
                initialVelocity = velocityLeft,
            )
        }

        return consumeVelocityIfNotAtScrollEdge(velocityLeft)
    }

    private suspend fun ScrollScope.performDecayFling(
        initialItem: SnapperLayoutItemInfo,
        targetIndex: Int,
        initialVelocity: Float,
        flingThenSpring: Boolean = true,
    ): Float {
        if (initialItem.index == targetIndex && layoutInfo.distanceToIndexSnap(initialItem.index) == 0) {
            return consumeVelocityIfNotAtScrollEdge(initialVelocity)
        }

        var velocityLeft = initialVelocity
        var lastValue = 0f

        val canSpringThenFling = flingThenSpring && abs(targetIndex - initialItem.index) >= 2

        try {
            animationTarget = targetIndex

            AnimationState(
                initialValue = 0f,
                initialVelocity = initialVelocity,
            ).animateDecay(decayAnimationSpec) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = velocity

                if (abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }

                val currentItem = layoutInfo.currentItem
                if (currentItem == null) {
                    cancelAnimation()
                    return@animateDecay
                }

                if (isRunning && canSpringThenFling) {
                    if (velocity > 0 && currentItem.index == targetIndex - 1) {
                        cancelAnimation()
                    } else if (velocity < 0 && currentItem.index == targetIndex) {
                        cancelAnimation()
                    }
                }

                if (isRunning && performSnapBackIfNeeded(
                        currentItem,
                        targetIndex,
                        ::scrollBy
                    )
                ) {
                    cancelAnimation()
                }
            }
        } finally {
            animationTarget = null
        }

        return velocityLeft
    }

    private suspend fun ScrollScope.performSpringFling(
        initialItem: SnapperLayoutItemInfo,
        targetIndex: Int,
        initialVelocity: Float = 0f,
    ): Float {
        var velocityLeft = when {
            targetIndex > initialItem.index && initialVelocity > 0 -> initialVelocity
            targetIndex <= initialItem.index && initialVelocity < 0 -> initialVelocity
            else -> 0f
        }
        var lastValue = 0f

        try {
            animationTarget = targetIndex

            AnimationState(
                initialValue = lastValue,
                initialVelocity = velocityLeft,
            ).animateTo(
                targetValue = layoutInfo.distanceToIndexSnap(targetIndex).toFloat(),
                animationSpec = springAnimationSpec,
            ) {
                val delta = value - lastValue
                val consumed = scrollBy(delta)
                lastValue = value
                velocityLeft = velocity

                val currentItem = layoutInfo.currentItem
                if (currentItem == null) {
                    cancelAnimation()
                    return@animateTo
                }

                if (performSnapBackIfNeeded(currentItem, targetIndex, ::scrollBy)) {
                    cancelAnimation()
                } else if (abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }
            }
        } finally {
            animationTarget = null
        }

        return velocityLeft
    }

    private fun AnimationScope<Float, AnimationVector1D>.performSnapBackIfNeeded(
        currentItem: SnapperLayoutItemInfo,
        targetIndex: Int,
        scrollBy: (pixels: Float) -> Float,
    ): Boolean {
        val snapBackAmount = calculateSnapBack(velocity, currentItem, targetIndex)

        if (snapBackAmount != 0) {
            scrollBy(snapBackAmount.toFloat())
            return true
        }

        return false
    }

    private fun DecayAnimationSpec<Float>.canDecayBeyondCurrentItem(
        velocity: Float,
        currentItem: SnapperLayoutItemInfo,
    ): Boolean {
        if (velocity.absoluteValue < 0.5f) return false

        val flingDistance = calculateTargetValue(0f, velocity)

        return if (velocity < 0) {
            flingDistance <= layoutInfo.distanceToIndexSnap(currentItem.index)
        } else {
            flingDistance >= layoutInfo.distanceToIndexSnap(currentItem.index + 1)
        }
    }

    private fun calculateSnapBack(
        initialVelocity: Float,
        currentItem: SnapperLayoutItemInfo,
        targetIndex: Int,
    ): Int = when {
        initialVelocity > 0 && currentItem.index >= targetIndex -> {
            layoutInfo.distanceToIndexSnap(currentItem.index)
        }

        initialVelocity < 0 && currentItem.index <= targetIndex - 1 -> {
            layoutInfo.distanceToIndexSnap(currentItem.index + 1)
        }

        else -> 0
    }

    private fun consumeVelocityIfNotAtScrollEdge(velocity: Float): Float {
        if (velocity < 0 && !layoutInfo.canScrollTowardsStart()) {
            return velocity
        } else if (velocity > 0 && !layoutInfo.canScrollTowardsEnd()) {
            return velocity
        }
        return 0f
    }
}