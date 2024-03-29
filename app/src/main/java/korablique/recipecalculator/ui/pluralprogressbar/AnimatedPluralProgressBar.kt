package korablique.recipecalculator.ui.pluralprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import korablique.recipecalculator.TestEnvironmentDetector

/**
 * Animates progress changes.
 */
class AnimatedPluralProgressBar : PluralProgressBar {
    private var displayedProgress = emptyArray<Float>()
    private var progressAnimators = emptyArray<ValueAnimator>()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /**
     * Whether progress changes are animated. If false, the AnimatedPluralProgressBar
     * instance effectively becomes PluralProgressBar.
     */
    var animationsEnabled = true

    override fun setProgress(progress: Array<Float>) {
        if (!animationsEnabled || TestEnvironmentDetector.isInTests()) {
            super.setProgress(progress)
            return
        }

        // Cancel last animation
        progressAnimators.forEach { it.cancel() }

        if (progressAnimators.isEmpty()) {
            progressAnimators = Array(progress.size, init = { ValueAnimator() })
        }
        if (displayedProgress.isEmpty()) {
            displayedProgress = Array(progress.size, init = { 0f })
        }

        for (index in 0 until progress.size) {
            progressAnimators[index] = ValueAnimator.ofFloat(displayedProgress[index], progress[index])

            // Max duration is 1000 milliseconds, duration used for animation is calculated by the
            // difference between displayedProgress and given to the function progress.
            // E.g. if realProgress==100f && displayedProgress==50f, duration would be 500.
            val duration = 1000 * 0.01 * Math.abs(displayedProgress[index] - progress[index])
            progressAnimators[index].duration = duration.toLong()

            // Let's start the animation!
            progressAnimators[index].addUpdateListener {
                displayedProgress[index] = progressAnimators[index].animatedValue as Float
                setProgressImpl(displayedProgress, throwIfInvalidSum = false)
            }

            var finishedAnimations = 0
            progressAnimators[index].addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) {
                    finishedAnimations += 1
                    if (finishedAnimations == progress.size) {
                        this@AnimatedPluralProgressBar.setProgressImpl(
                                displayedProgress, throwIfInvalidSum = true)
                    }
                }
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            progressAnimators[index].start()
        }
    }
}