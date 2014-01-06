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
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import me.eugeniomarletti.tetheringfixer.R;

import static me.eugeniomarletti.tetheringfixer.Utils.getInterpolatorFromStyle;

public final class BulletExpandEffect extends View
{
    private static final int DEFAULT_STYLE      = R.style.BulletExpandEffectStyle;
    private static final int DEFAULT_STYLE_ATTR = R.attr.bulletExpandEffectStyle;

    private Interpolator  interpolator;
    private int           duration;
    private ValueAnimator animator;
    private Paint         paint;
    private PointF        center;
    private float         size;
    private float         finalSize;
    private boolean       fullyExpanded;

    public BulletExpandEffect(Context context)
    {
        this(context, null);
    }

    public BulletExpandEffect(Context context, AttributeSet attrs)
    {
        this(context, attrs, DEFAULT_STYLE_ATTR);
    }

    public BulletExpandEffect(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        TypedArray a = null;
        try
        {
            a = context.obtainStyledAttributes(attrs, R.styleable.BulletExpandEffect, defStyleAttr, DEFAULT_STYLE);
            interpolator = getInterpolatorFromStyle(context, a, R.styleable.BulletExpandEffect_interpolator, null);
            duration = a.getInt(R.styleable.BulletExpandEffect_duration, 0);
        }
        finally
        {
            if (a != null) a.recycle();
        }
    }

    public Interpolator getInterpolator()
    {
        return interpolator;
    }

    public void setInterpolator(Interpolator interpolator)
    {
        if (this.interpolator != interpolator)
        {
            this.interpolator = interpolator;
            if (animator != null) animator.setInterpolator(interpolator);
        }
    }

    public int getDuration()
    {
        return duration;
    }

    public void setDuration(int duration)
    {
        if (this.duration != duration)
        {
            this.duration = duration;
            if (animator != null) animator.setDuration(duration);
        }
    }

    public void animateExpand(int color, PointF center, float startSize, Float finalSize,
                              final Runnable onAnimationFinish)
    {
        if (center == null) center = new PointF();
        this.center = center;
        size = startSize;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);

        if (finalSize != null) this.finalSize = finalSize;
        else
        {
            Rect localVisibleRect = new Rect();
            getLocalVisibleRect(localVisibleRect);
            RectF localVisibleRectF = new RectF(localVisibleRect);
            localVisibleRectF.offsetTo(-center.x, -center.y);
            this.finalSize = Math.max(Math.max(new PointF(localVisibleRectF.left, localVisibleRectF.top).length(),
                                               new PointF(localVisibleRectF.right, localVisibleRectF.top).length()),
                                      Math.max(new PointF(localVisibleRectF.left, localVisibleRectF.bottom).length(),
                                               new PointF(localVisibleRectF.right, localVisibleRectF.bottom).length()));
        }

        animator = ValueAnimator.ofFloat(startSize, this.finalSize);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator)
            {
                final float newSize = (float)valueAnimator.getAnimatedValue();
                if (size != newSize)
                {
                    size = newSize;
                    fullyExpanded = size == BulletExpandEffect.this.finalSize;
                    postInvalidateOnAnimation();
                }
            }
        });
        animator.addListener(new SimpleAnimatorListener()
        {
            @Override
            public void onAnimationEnd(Animator animator)
            {
                animator.removeListener(this);
                BulletExpandEffect.this.animator = null;
                BulletExpandEffect.this.center = null;
                BulletExpandEffect.this.paint = null;
                if (onAnimationFinish != null) onAnimationFinish.run();
            }
        });
        animator.start();
    }

    public void animateExpand(int color, PointF center, float startSize, final Runnable onAnimationFinish)
    {
        animateExpand(color, center, startSize, null, onAnimationFinish);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (fullyExpanded) canvas.drawColor(paint.getColor());
        else if (size > 0f) canvas.drawCircle(center.x, center.y, size, paint);
    }
}
