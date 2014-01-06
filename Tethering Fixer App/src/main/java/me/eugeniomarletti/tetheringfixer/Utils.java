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

package me.eugeniomarletti.tetheringfixer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static android.view.View.MEASURED_STATE_TOO_SMALL;
import static android.view.View.MeasureSpec;

public final class Utils
{
    public static void testCrash()
    {
        Utils.runOnMainThread(new Runnable()
        {
            @Override
            public void run()
            {
                throw new RuntimeException("Test crash!");
            }
        }, false);
    }

    public static boolean isMainThread()
    {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    // used by runOnMainThread()
    public static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable runnable, boolean check)
    {
        if (check && isMainThread()) runnable.run();
        else MAIN_THREAD_HANDLER.post(runnable);
    }

    public static void runOnMainThread(Runnable runnable)
    {
        runOnMainThread(runnable, true);
    }

    public static Interpolator getInterpolatorFromStyle(Context context, TypedArray array, int index,
                                                        Interpolator defaultValue)
    {
        final int id = array.getResourceId(index, 0);
        if (id == 0) return defaultValue;
        return AnimationUtils.loadInterpolator(context, id);
    }

    public static int getResolvedSizeAndState(int size, int measureSpec)
    {
        int result;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode)
        {
            default:
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = specSize < size ? specSize | MEASURED_STATE_TOO_SMALL : size;
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    public static float dp2px(float dp, DisplayMetrics displayMetrics)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    public static Point getLocationInWindow(View view)
    {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Point(location[0], location[1]);
    }

    public static Point getOffsetBetweenViews(View from, View to)
    {
        int[] fromLocation = new int[2];
        int[] toLocation = new int[2];
        from.getLocationInWindow(fromLocation);
        to.getLocationInWindow(toLocation);
        return new Point(toLocation[0] - fromLocation[0],
                         toLocation[1] - fromLocation[1]);
    }

    @SuppressWarnings("ConstantConditions")
    public static ViewTreeObserver.OnGlobalLayoutListener
    addOnGlobalLayoutListener(final View view, final boolean removeListener,
                              final ViewTreeObserver.OnGlobalLayoutListener listener)
    {
        final ViewTreeObserver vto = view.getViewTreeObserver();
        final ViewTreeObserver.OnGlobalLayoutListener wrapper = !removeListener ? listener : new ViewTreeObserver
                .OnGlobalLayoutListener()
        {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout()
            {
                final ViewTreeObserver _vto = vto.isAlive() ? vto : view.getViewTreeObserver();
                if (Build.VERSION.SDK_INT >= 16) _vto.removeOnGlobalLayoutListener(this);
                else _vto.removeGlobalOnLayoutListener(this);
                listener.onGlobalLayout();
            }
        };
        vto.addOnGlobalLayoutListener(wrapper);
        return wrapper;
    }

    // used by generateViewId()
    private static final AtomicInteger nextGeneratedId = Build.VERSION.SDK_INT >= 17 ? null : new AtomicInteger(1);

    /**
     * Generate a value suitable for use in setId(int).
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId()
    {
        if (Build.VERSION.SDK_INT >= 17) return View.generateViewId();
        else
        {
            while (true)
            {
                final int result = nextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (nextGeneratedId.compareAndSet(result, newValue)) return result;
            }
        }
    }

    // used by addEmoji()
    private static final Pattern REGEX_SMILE_HAPPY = Pattern.compile("(?<=^|\\s):\\)(?=\\s|$)");
    private static final Pattern REGEX_SMILE_SAD   = Pattern.compile("(?<=^|\\s):\\((?=\\s|$)");
    private static final String  EMOJI_HAPPY       = "🙌";
    private static final String  EMOJI_SAD         = "😱";

    public static String addEmoji(CharSequence in)
    {
        return in == null ? null : REGEX_SMILE_SAD.matcher(REGEX_SMILE_HAPPY.matcher(in)
                                                                            .replaceAll(" " + EMOJI_HAPPY))
                                                  .replaceAll(" " + EMOJI_SAD);
    }
}
