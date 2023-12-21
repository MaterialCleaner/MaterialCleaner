package me.gm.cleaner.widget.recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.function.Predicate

open class AlphaAwareItemAnimator(
    private val translucentAlpha: Float = 0.5F /* disabled_alpha_device_default */
) : OverridableDefaultItemAnimator() {
    open val isTranslucent: Predicate<RecyclerView.ViewHolder> =
        Predicate<RecyclerView.ViewHolder> { false }

    init {
        supportsChangeAnimations = false
    }

    override fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        mAddAnimations.add(holder)
        val view = holder.itemView
        val animation = view.animate()
        val alpha = if (isTranslucent.test(holder)) translucentAlpha else 1F
        animation
            .alpha(alpha)
            .setDuration(addDuration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {
                    dispatchAddStarting(holder)
                }

                override fun onAnimationCancel(animator: Animator) {
                    view.alpha = alpha
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                    dispatchAddFinished(holder)
                    mAddAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }
}
