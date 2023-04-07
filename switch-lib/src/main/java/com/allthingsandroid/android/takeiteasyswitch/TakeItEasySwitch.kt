package com.allthingsandroid.android.takeiteasyswitch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.DrawableUtils
import androidx.appcompat.widget.SwitchCompat
import com.crazylegend.gson.toJsonPrettyPrinting
import com.crazylegend.view.pxToDp
import java.lang.reflect.Field
import java.lang.reflect.Method

open class TakeItEasySwitch: SwitchCompat {

    companion object{

        private val TAG = this::class.java.declaringClass.simpleName

        private val logsEnabled = BuildConfig.DEBUG

        private var _mSwitchWidthField: Field? = null
        private val mSwitchWidthField: Field get() = _mSwitchWidthField!!

        private var _mSwitchMinWidthField: Field? = null
        private val mSwitchMinWidthField: Field get() = _mSwitchMinWidthField!!

        private var _mSwitchTopField: Field? = null
        private val mSwitchTopField: Field get() = _mSwitchTopField!!

        private var _mSwitchLeftField: Field? = null
        private val mSwitchLeftField: Field get() = _mSwitchLeftField!!

        private var _mSwitchRightField: Field? = null
        private val mSwitchRightField: Field get() = _mSwitchRightField!!

        private var _mSwitchBottomField: Field? = null
        private val mSwitchBottomField: Field get() = _mSwitchBottomField!!

        private var _mSwitchHeightField: Field? = null
        private val mSwitchHeightField: Field get() = _mSwitchHeightField!!

        private var _mThumbDrawableField: Field? = null
        private val mThumbDrawableField: Field get() = _mThumbDrawableField!!

        private var _mThumbWidthField: Field? = null
        private val mThumbWidthField: Field get() = _mThumbWidthField!!

        private var _mTrackDrawableField: Field? = null
        private val mTrackDrawableField: Field get() = _mTrackDrawableField!!

        private var _mGetThumbOffsetMethod: Method? = null
        private val mGetThumbOffsetMethod: Method get() = _mGetThumbOffsetMethod!!

        init {
            try {
                SwitchCompat::class.java.apply {
                    _mSwitchWidthField = getDeclaredField("mSwitchWidth")
                    _mSwitchMinWidthField = getDeclaredField("mSwitchMinWidth")
                    _mSwitchHeightField = getDeclaredField("mSwitchHeight")
                    _mSwitchTopField = getDeclaredField("mSwitchTop")
                    _mSwitchLeftField = getDeclaredField("mSwitchLeft")
                    _mSwitchRightField = getDeclaredField("mSwitchRight")
                    _mSwitchBottomField = getDeclaredField("mSwitchBottom")

                    _mThumbDrawableField = getDeclaredField("mThumbDrawable")
                    _mThumbWidthField = getDeclaredField("mThumbWidth")

                    _mTrackDrawableField = getDeclaredField("mTrackDrawable")

                    _mGetThumbOffsetMethod = getDeclaredMethod("getThumbOffset")
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            val allReflectiveFields = setOf(
                _mSwitchWidthField,
                _mSwitchMinWidthField,
                _mSwitchHeightField,
                _mSwitchTopField,
                _mSwitchLeftField,
                _mSwitchRightField,
                _mSwitchBottomField,
                _mThumbDrawableField,
                _mThumbWidthField,
                _mTrackDrawableField,
                _mGetThumbOffsetMethod
            )
            val allReflectiveFieldsAvailable = allReflectiveFields.all {
                it != null
            }
            if(!allReflectiveFieldsAvailable){
                throw IllegalStateException("Some fields are not available through reflection." +
                        " This class can't function without them. Consider patching this class or" +
                        " using some other Switch")
            }

            allReflectiveFields.forEach {
                it!!.isAccessible = true
            }
        }
    }



    private val mTempRect = Rect()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onTouchEvent(ev: MotionEvent): Boolean{
        Log.d(TAG, "onTouchEvent() called")
        return super.onTouchEvent(ev)
    }


    @SuppressLint("RestrictedApi")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val padding = mTempRect
        val thumbWidth: Int
        val thumbHeight: Int
        val thumbDrawable = mThumbDrawableField.asDrawable()
        if (thumbDrawable != null) {
            // Cached thumb width does not include padding.
            thumbDrawable.getPadding(padding)
            thumbWidth = thumbDrawable.intrinsicWidth - padding.left - padding.right
            thumbHeight = thumbDrawable.intrinsicHeight
        } else {
            thumbWidth = 0
            thumbHeight = 0
        }

        // Here, we skip a calculation that takes into account the width of
        // the switch text
        // It used to be,
        //mThumbWidth = Math.max(maxTextWidth, thumbWidth)

        // Now,
        mThumbWidthField.set(this, thumbWidth)

        //[Copied from super()]
        // Adjust left and right padding to ensure there's enough room for the
        // thumb's padding (when present).
        var paddingLeft = padding.left
        var paddingRight = padding.right
        if (thumbDrawable != null) {
            val inset = DrawableUtils.getOpticalBounds(thumbDrawable)
            paddingLeft = Math.max(paddingLeft, inset.left)
            paddingRight = Math.max(paddingRight, inset.right)
        }

        // We have the changed the track width calculation that used to have the switchWidth
        // to be more than 2 * mThumbWidth
        val trackWidth = mTrackDrawableField.asDrawable()?.intrinsicWidth
        val switchWidth = Math.max(
            mSwitchMinWidthField.asInt(),
            trackWidth ?: (2 * mThumbWidthField.asInt() + paddingLeft + paddingRight)
        )
        mSwitchWidthField.set(this, switchWidth)
    }

    @SuppressLint("RestrictedApi")
    override fun onDraw(c: Canvas?) {

        val padding = mTempRect
        val switchLeft = mSwitchLeftField.asInt()
        val switchTop = mSwitchTopField.asInt()
        //val switchRight = mSwitchRight
        //val switchBottom = mSwitchBottom
        var thumbInitialLeft = switchLeft + mGetThumbOffsetMethod.invoke(this) as Int

        // Layout the thumb.
        val thumbDrawable = mThumbDrawableField.asDrawable()
        if (thumbDrawable != null) {
            thumbDrawable.getPadding(padding)
            val thumbLeft = thumbInitialLeft - padding.left
            val thumbRight = thumbInitialLeft + mThumbWidthField.asInt() + padding.right
            val diffLength: Int
            val availableHeight = mSwitchHeightField.asInt() - paddingTop - paddingBottom
            if(availableHeight >= thumbDrawable.intrinsicHeight){
                diffLength = (availableHeight - thumbDrawable.intrinsicHeight)/2
            }
            else{
                diffLength = 0
            }
            val thumbTop = switchTop + diffLength
            val thumbBottom = switchTop + availableHeight - diffLength
            thumbDrawable.setBounds(thumbLeft, thumbTop, thumbRight, thumbBottom)
            //thumbDrawable.setBounds(10, 10, 10, 10)

            if(logsEnabled){
                val mapSwitchState = mapOf(
                    "Switch" to mapOf (
                        "mSwitchWidth" to mSwitchWidthField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "mSwitchHeight" to mSwitchHeightField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "mSwitchTop" to mSwitchTopField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "mSwitchLeft" to mSwitchLeftField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "mSwitchRight" to mSwitchRightField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "mSwitchBottom" to mSwitchBottomField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "switchPadding" to "[paddingBottom = ${paddingBottom.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingTop = ${paddingTop.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingRight = ${paddingRight.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingLeft = ${paddingLeft.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}]"
                    ),
                    "Thumb" to mapOf (
                        "mThumbWidth" to mThumbWidthField.asInt().let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "thumbDrawableIntrinsicWidth" to mThumbDrawableField.asDrawable()?.intrinsicWidth?.let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "thumbDrawableIntrinsicHeight" to mThumbDrawableField.asDrawable()?.intrinsicHeight?.let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "thumbDrawablePadding" to "[paddingBottom = ${padding.bottom.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingTop = ${padding.top.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingRight = ${padding.right.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, paddingLeft = ${padding.left.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}]",
                        "thumbDrawableBounds" to "[thumbBottom = ${thumbBottom.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, thumbTop = ${thumbTop.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, thumbRight = ${thumbRight.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}, thumbLeft = ${thumbLeft.let { arrayOf("$it px", "${it.pxToDp()} dp").joinToString() }}]"
                    ),
                    "Track" to mapOf (
                        "trackDrawableIntrinsicWidth" to mTrackDrawableField.asDrawable()?.intrinsicWidth?.let { arrayOf("$it px", "${it.pxToDp()} dp") },
                        "trackDrawableIntrinsicHeight" to mTrackDrawableField.asDrawable()?.intrinsicHeight?.let { arrayOf("$it px", "${it.pxToDp()} dp") },
                    ),
                )
                Log.d(TAG, mapSwitchState.toJsonPrettyPrinting())
            }

        }
        super.onDraw(c)

    }

    private fun Field.asDrawable() = get(this@TakeItEasySwitch) as Drawable?
    private fun Field.asInt() = get(this@TakeItEasySwitch) as Int
}