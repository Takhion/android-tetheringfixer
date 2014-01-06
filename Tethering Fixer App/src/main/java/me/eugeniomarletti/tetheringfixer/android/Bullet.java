/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Eugenio Marletti
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.eugeniomarletti.tetheringfixer.android;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import me.eugeniomarletti.tetheringfixer.R;

import static me.eugeniomarletti.tetheringfixer.Utils.getInterpolatorFromStyle;
import static me.eugeniomarletti.tetheringfixer.Utils.getResolvedSizeAndState;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.*;

public final class Bullet extends View
        implements Checkable, StateHolderProvider, StateErrorHolder.StateError, StateWorkingHolder.StateWorking
{
    public static final int DEFAULT_STYLE      = R.style.BulletStyle; // default style
    public static final int DEFAULT_STYLE_ATTR = R.attr.bulletStyle; // attribute for theme style

    private float bulletSize;
    private float ringSize;

    private final Paint bulletPaint = new Paint();
    private final Paint ringPaint   = new Paint();

    private final ValueAnimator bulletSizeAnimator = new ValueAnimator();
    private final TimeInterpolator bulletSizeAnimationInterpolator;
    private final int              bulletSizeAnimationDuration;
    private final float            bulletSizeActivated;
    private final float            bulletSizeNotActivated;

    private final ValueAnimator bulletColorAnimator = new ValueAnimator();
    private final TimeInterpolator bulletColorAnimationInterpolator;
    private final int              bulletColorAnimationDuration;
    private final int              bulletColorError;
    private final int              bulletColorChecked;
    private final int              bulletColorNormal;

    private final ValueAnimator ringSizeAnimator = new ValueAnimator();
    private final TimeInterpolator ringSizeAnimationInterpolator;
    private final float            ringSizeStart;
    private final float            ringSizeEnd;

    private final ValueAnimator ringThicknessAnimator = new ValueAnimator();
    private final TimeInterpolator ringThicknessAnimationInterpolator;
    private final float            ringThicknessStart;
    private final float            ringThicknessEnd;

    private final ValueAnimator ringAlphaAnimator = new ValueAnimator();
    private final TimeInterpolator ringAlphaAnimationInterpolator;
    private final float            ringAlphaStart;
    private final float            ringAlphaEnd;

    private final AnimatorSet ringAnimatorSet = new AnimatorSet();
    private final int ringAnimationDuration;
    private final int ringAnimationPauseDuration;
    private boolean ringAnimationContinue = false;

    private boolean wasError;      // red
    private boolean wasWorking;    // ring
    private boolean wasActivated;  // big
    private boolean wasChecked;    // blue

    private final StateHolder   stateChecked = new StateCheckedHolder(this);
    private final StateHolder   stateError   = new StateErrorHolder(this);
    private final StateHolder   stateWorking = new StateWorkingHolder(this);
    private final StateHolder[] states       = new StateHolder[] { stateChecked, stateError, stateWorking };

    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    public Bullet(Context context)
    {
        this(context, null);
    }

    public Bullet(Context context, AttributeSet attrs)
    {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    @SuppressWarnings("ConstantConditions")
    public Bullet(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        TypedArray a = null;
        try
        {
            a = context.obtainStyledAttributes(attrs, R.styleable.Bullet, defStyleAttr, DEFAULT_STYLE);

            bulletSizeAnimationInterpolator =
                    getInterpolatorFromStyle(context, a, R.styleable.Bullet_bulletSizeAnimationInterpolator, null);
            bulletSizeAnimationDuration = a.getInt(R.styleable.Bullet_bulletSizeAnimationDuration, 0);
            bulletSizeActivated = a.getDimension(R.styleable.Bullet_bulletSizeActivated, 0f);
            bulletSizeNotActivated = a.getDimension(R.styleable.Bullet_bulletSizeNotActivated, 0f);

            bulletColorAnimationInterpolator =
                    getInterpolatorFromStyle(context, a, R.styleable.Bullet_bulletColorAnimationInterpolator, null);
            bulletColorAnimationDuration = a.getInt(R.styleable.Bullet_bulletColorAnimationDuration, 0);
            bulletColorError = a.getColor(R.styleable.Bullet_bulletColorError, 0);
            bulletColorChecked = a.getColor(R.styleable.Bullet_bulletColorChecked, 0);
            bulletColorNormal = a.getColor(R.styleable.Bullet_bulletColorNormal, 0);

            ringSizeAnimationInterpolator =
                    getInterpolatorFromStyle(context, a, R.styleable.Bullet_ringSizeAnimationInterpolator, null);
            ringSizeStart = a.getDimension(R.styleable.Bullet_ringSizeStart, 0f);
            ringSizeEnd = a.getDimension(R.styleable.Bullet_ringSizeEnd, 0f);

            ringThicknessAnimationInterpolator =
                    getInterpolatorFromStyle(context, a, R.styleable.Bullet_ringThicknessAnimationInterpolator, null);
            ringThicknessStart = a.getDimension(R.styleable.Bullet_ringThicknessStart, 0f);
            ringThicknessEnd = a.getDimension(R.styleable.Bullet_ringThicknessEnd, 0f);

            ringAlphaAnimationInterpolator =
                    getInterpolatorFromStyle(context, a, R.styleable.Bullet_ringAlphaAnimationInterpolator, null);
            ringAlphaStart = a.getFloat(R.styleable.Bullet_ringAlphaStart, 0f);
            ringAlphaEnd = a.getFloat(R.styleable.Bullet_ringAlphaEnd, 0f);

            ringAnimationDuration = a.getInt(R.styleable.Bullet_ringAnimationDuration, 0);
            ringAnimationPauseDuration = a.getInt(R.styleable.Bullet_ringAnimationPauseDuration, 0);

            wasError = a.getBoolean(R.styleable.Bullet_isError, false);
            wasWorking = a.getBoolean(R.styleable.Bullet_isWorking, false);
            wasActivated = a.getBoolean(R.styleable.Bullet_isActivated, false);
            wasChecked = a.getBoolean(R.styleable.Bullet_isChecked, false);
        }
        finally
        {
            if (a != null) a.recycle();
        }

        initialize();
    }

    private void initialize()
    {
        bulletPaint.setAntiAlias(true);
        ringPaint.setAntiAlias(true);
        ringPaint.setStyle(Paint.Style.STROKE);
        setBulletSize(wasActivated ? bulletSizeActivated : bulletSizeNotActivated);
        setBulletColor(wasError ? bulletColorError : (wasChecked ? bulletColorChecked : bulletColorNormal));

        bulletSizeAnimator.setDuration(bulletSizeAnimationDuration);
        bulletSizeAnimator.setInterpolator(bulletSizeAnimationInterpolator);
        bulletSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                if (setBulletSize((float)valueAnimator.getAnimatedValue())) postInvalidateOnAnimation();
            }
        });

        bulletColorAnimator.setDuration(bulletColorAnimationDuration);
        bulletColorAnimator.setInterpolator(bulletColorAnimationInterpolator);
        bulletColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                if (setBulletColor((int)valueAnimator.getAnimatedValue())) postInvalidateOnAnimation();
            }
        });

        ringSizeAnimator.setFloatValues(ringSizeStart, ringSizeEnd);
        ringSizeAnimator.setInterpolator(ringSizeAnimationInterpolator);
        ringSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                if (setRingSize((float)valueAnimator.getAnimatedValue())) postInvalidateOnAnimation();
            }
        });

        ringThicknessAnimator.setFloatValues(ringThicknessStart, ringThicknessEnd);
        ringThicknessAnimator.setInterpolator(ringThicknessAnimationInterpolator);
        ringThicknessAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                if (setRingThickness((float)valueAnimator.getAnimatedValue())) postInvalidateOnAnimation();
            }
        });

        ringAlphaAnimator.setFloatValues(ringAlphaStart, ringAlphaEnd);
        ringAlphaAnimator.setInterpolator(ringAlphaAnimationInterpolator);
        ringAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                if (setRingAlpha((float)valueAnimator.getAnimatedValue())) postInvalidateOnAnimation();
            }
        });

        //noinspection ConstantConditions
        ringAnimatorSet.playTogether(ringSizeAnimator, ringThicknessAnimator, ringAlphaAnimator);
        ringAnimatorSet.setDuration(ringAnimationDuration);
        ringAnimatorSet.addListener(new SimpleAnimatorListener()
        {
            @Override
            public void onAnimationEnd(Animator animator)
            {
                if (ringAnimationContinue) postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (ringAnimationContinue) ringAnimatorSet.start();
                    }
                }, ringAnimationPauseDuration);
            }
        });

        stateError.setState(wasError);
        stateWorking.setState(wasWorking);
        setActivated(wasActivated);
        setChecked(wasChecked);
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        final boolean isError = isError();
        final boolean wasError = this.wasError;
        final boolean changedError = isError != wasError;
        this.wasError = isError;

        final boolean isWorking = isWorking();
        final boolean wasWorking = this.wasWorking;
        final boolean changedWorking = isWorking != wasWorking;
        this.wasWorking = isWorking;

        final boolean isChecked = isChecked();
        final boolean wasChecked = this.wasChecked;
        final boolean changedChecked = isChecked != wasChecked;
        this.wasChecked = isChecked;

        final boolean isActivated = isActivated();
        final boolean wasActivated = this.wasActivated;
        final boolean changedActivated = isActivated != wasActivated;
        this.wasActivated = isActivated;

        // error / checked
        Integer newColor = null;
        if (changedError && isError) newColor = bulletColorError;
        else if (changedChecked && isChecked) newColor = bulletColorChecked;
        else if (changedError || changedChecked) newColor = bulletColorNormal;
        if (newColor != null) animateBulletColor(newColor);

        // activated
        if (changedActivated) animateBulletSize(isActivated ? bulletSizeActivated : bulletSizeNotActivated);

        // working
        if (changedWorking)
        {
            if (isWorking) startRingAnimation();
            else stopRingAnimation();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        final int[] additionalState = getAdditionalState(this);
        return mergeDrawableStates(super.onCreateDrawableState(extraSpace + additionalState.length), additionalState);
    }

    @Override
    public StateHolder[] getStateHolders()
    {
        return states;
    }

    public boolean isError()
    {
        return stateError.isState();
    }

    public void setError(boolean isError)
    {
        stateError.setState(isError);
    }

    @Override
    public void toggleError()
    {
        stateError.toggleState();
    }

    public boolean isWorking()
    {
        return stateWorking.isState();
    }

    public void setWorking(boolean isWorking)
    {
        stateWorking.setState(isWorking);
    }

    @Override
    public void toggleWorking()
    {
        stateWorking.toggleState();
    }

    @Override
    public boolean isChecked()
    {
        return stateChecked.isState();
    }

    @Override
    public void setChecked(boolean b)
    {
        stateChecked.setState(b);
    }

    @Override
    public void toggle()
    {
        stateChecked.toggleState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        final int ringSize = (int)Math.ceil(ringSizeEnd);
        final int desiredWidth = Math.max(getMinimumWidth(), ringSize);
        final int desiredHeight = Math.max(getMinimumHeight(), ringSize);
        final int width = getResolvedSizeAndState(desiredWidth, widthMeasureSpec);
        final int height = getResolvedSizeAndState(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        final float bulletRadius = getBulletSize() / 2f;
        final float ringThickness = getRingThickness();
        final float ringRadius = (getRingSize() - ringThickness) / 2f;

        canvas.save();
        canvas.translate(canvas.getWidth() / 2f, canvas.getHeight() / 2f);
        if (ringThickness > 0f
                && (ringSize > bulletSize
                || ringThickness * 2f - ringSize > bulletSize
                || bulletPaint.getAlpha() < 255))
            canvas.drawCircle(0f, 0f, ringRadius, ringPaint);
        canvas.drawCircle(0f, 0f, bulletRadius, bulletPaint);
        canvas.restore();
    }

    private void animateBulletSize(float newSize)
    {
        if (bulletSizeAnimator.isStarted()) bulletSizeAnimator.cancel();
        bulletSizeAnimator.setFloatValues(getBulletSize(), newSize);
        bulletSizeAnimator.start();
    }

    private void animateBulletColor(int newColor)
    {
        if (bulletColorAnimator.isStarted()) bulletColorAnimator.cancel();
        bulletColorAnimator.setValues(
                PropertyValuesHolder.ofObject("", ARGB_EVALUATOR, getBulletColor(), newColor));
        bulletColorAnimator.start();
    }

    private void startRingAnimation()
    {
        ringAnimationContinue = true;
        if (!ringAnimatorSet.isStarted()) ringAnimatorSet.start();
    }

    private void stopRingAnimation()
    {
        ringAnimationContinue = false;
    }

    public int getBulletColor()
    {
        return bulletPaint.getColor();
    }

    private boolean setBulletColor(int color)
    {
        if (getBulletColor() != color)
        {
            final float ringAlpha = getRingAlpha();
            bulletPaint.setColor(color);
            ringPaint.setColor(color);
            setRingAlpha(ringAlpha);
            return true;
        }
        return false;
    }

    public float getRingAlpha()
    {
        return ringPaint.getAlpha() / 255f;
    }

    private boolean setRingAlpha(float alpha)
    {
        final int _alpha = Math.round(alpha * 255);
        if (ringPaint.getAlpha() != _alpha)
        {
            ringPaint.setAlpha(_alpha);
            return true;
        }
        return false;
    }

    public float getRingThickness()
    {
        return ringPaint.getStrokeWidth();
    }

    private boolean setRingThickness(float thickness)
    {
        if (getRingThickness() != thickness)
        {
            ringPaint.setStrokeWidth(thickness);
            return true;
        }
        return false;
    }

    public float getRingSize()
    {
        return ringSize;
    }

    private boolean setRingSize(float size)
    {
        if (getRingSize() != size)
        {
            ringSize = size;
            return true;
        }
        return false;
    }

    public float getBulletSize()
    {
        return bulletSize;
    }

    private boolean setBulletSize(float size)
    {
        if (getBulletSize() != size)
        {
            bulletSize = size;
            return true;
        }
        return false;
    }

    public int getBulletColorChecked()
    {
        return bulletColorChecked;
    }

    public PointF getBulletCenter()
    {
        return new PointF(getWidth() / 2f, getHeight() / 2f);
    }

    public void addBulletColorAnimationListener(Animator.AnimatorListener listener)
    {
        bulletColorAnimator.addListener(listener);
    }

    public void removeBulletColorAnimationListener(Animator.AnimatorListener listener)
    {
        bulletColorAnimator.removeListener(listener);
    }
}
