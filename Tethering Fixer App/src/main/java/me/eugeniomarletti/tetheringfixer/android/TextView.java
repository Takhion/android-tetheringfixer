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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

import static me.eugeniomarletti.tetheringfixer.android.DrawableState.StateCheckedHolder;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.StateErrorHolder;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.StateHolder;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.StateHolderProvider;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.StateWorkingHolder;
import static me.eugeniomarletti.tetheringfixer.android.DrawableState.getAdditionalState;

public final class TextView extends android.widget.TextView
        implements Checkable, StateHolderProvider, StateErrorHolder.StateError, StateWorkingHolder.StateWorking
{
    private final StateHolder   stateChecked = new StateCheckedHolder(this);
    private final StateHolder   stateError   = new StateErrorHolder(this);
    private final StateHolder   stateWorking = new StateWorkingHolder(this);
    private final StateHolder[] states       = new StateHolder[] { stateChecked, stateError, stateWorking };

    public TextView(Context context)
    {
        super(context);
    }

    public TextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public TextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
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
}
