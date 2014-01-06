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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import me.eugeniomarletti.tetheringfixer.R;

/**
 * A Switch that accepts the attribute "android:fontFamily" inside a style defined by the
 * attribute "android:switchTextAppearance".
 *
 * @attr ref android.R.styleable#TextView_fontFamily
 * @attr ref android.R.styleable#Switch_switchTextAppearance
 */
@TargetApi(16)
public final class Switch extends android.widget.Switch
{
    /**
     * Construct a new Switch with default styling.
     *
     * @param context The Context that will determine this widget's theming.
     */
    public Switch(Context context)
    {
        super(context);
    }

    /**
     * Construct a new Switch with default styling, overriding specific style
     * attributes as requested.
     *
     * @param context The Context that will determine this widget's theming.
     * @param attrs   Specification of attributes that should deviate from default styling.
     */
    public Switch(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * Construct a new Switch with a default style determined by the given theme attribute,
     * overriding specific style attributes as requested.
     *
     * @param context  The Context that will determine this widget's theming.
     * @param attrs    Specification of attributes that should deviate from the default styling.
     * @param defStyle An attribute ID within the active theme containing a reference to the
     *                 default style for this widget. e.g. android.R.attr.switchStyle.
     */
    public Switch(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the switch text color, size, style, font family, hint color, and
     * highlight color from the specified TextAppearance resource.
     *
     * @attr ref android.R.styleable#Switch_switchTextAppearance
     */
    @Override
    public void setSwitchTextAppearance(Context context, int resid)
    {
        super.setSwitchTextAppearance(context, resid);

        TypedArray appearance = null;
        try
        {
            appearance = context.obtainStyledAttributes(resid, R.styleable.Android);
            final String fontFamily = appearance.getString(R.styleable.Android_android_fontFamily);
            if (fontFamily != null)
            {
                final Typeface currentTypeface = getTypeface();
                final Typeface newTypeface = Typeface.create(fontFamily, currentTypeface != null
                                                                         ? getTypeface().getStyle() : Typeface.NORMAL);
                if (newTypeface != null) setSwitchTypeface(newTypeface);
            }
        }
        finally
        {
            if (appearance != null) appearance.recycle();
        }
    }
}
