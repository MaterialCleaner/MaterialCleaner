package me.gm.cleaner.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.annotation.AttrRes
import androidx.coordinatorlayout.R
import androidx.coordinatorlayout.widget.CoordinatorLayout
import kotlin.math.abs

class OrthogonallyNestedCoordinatorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    @SuppressLint("PrivateResource") @AttrRes defStyleAttr: Int = R.attr.coordinatorLayoutStyle
) : CoordinatorLayout(context, attrs, defStyleAttr) {
    private var mIsBeingDragged: Boolean = false
    private var mIsUnableToDrag: Boolean = false
    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledPagingTouchSlop

    /**
     * Position of the last motion event.
     */
    private var mLastMotionX: Float = 0F
    private var mLastMotionY: Float = 0F
    private var mInitialMotionX: Float = 0F
    private var mInitialMotionY: Float = 0F

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private var mActivePointerId: Int = INVALID_POINTER

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleInterceptTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    private fun handleInterceptTouchEvent(ev: MotionEvent) {
        val action = ev.actionMasked

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> resetTouch()
            else -> {
                if (mIsBeingDragged) {
                    return
                }
                if (mIsUnableToDrag) {
                    requestParentDisallowInterceptTouchEvent()
                    return
                }
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                ev.x.let {
                    mLastMotionX = it
                    mInitialMotionX = it
                }
                ev.y.let {
                    mLastMotionY = it
                    mInitialMotionY = it
                }
                mActivePointerId = ev.getPointerId(ev.actionIndex)
                mIsUnableToDrag = false
            }
            MotionEvent.ACTION_MOVE -> {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                val activePointerId = mActivePointerId
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    return
                }

                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    // Error processing scroll; pointer index for id {pointerIndex} not found.
                    // Did any MotionEvents get skipped?"
                    return
                }
                val x = ev.getX(pointerIndex)
                val dx = x - mLastMotionX
                val xDiff = abs(dx)
                val y = ev.getY(pointerIndex)
                val yDiff = abs(y - mInitialMotionY)

                if (xDiff * 0.5F > yDiff) {
                    if (xDiff > mTouchSlop) {
                        mIsBeingDragged = true
                        mLastMotionX = if (dx > 0) {
                            mInitialMotionX + mTouchSlop
                        } else {
                            mInitialMotionX - mTouchSlop
                        }
                        mLastMotionY = y
                    }
                } else {
                    if (yDiff > mTouchSlop) {
                        mIsUnableToDrag = true
                    }
                    requestParentDisallowInterceptTouchEvent()
                }
            }
        }
    }

    private fun resetTouch() {
        mActivePointerId = INVALID_POINTER
        mIsBeingDragged = false
        mIsUnableToDrag = false
    }

    private fun requestParentDisallowInterceptTouchEvent() {
        parent.requestDisallowInterceptTouchEvent(true)
    }

    companion object {
        /**
         * Sentinel value for no current active pointer.
         * Used by [.mActivePointerId].
         */
        private const val INVALID_POINTER: Int = -1
    }
}
