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

import android.view.View;
import me.eugeniomarletti.tetheringfixer.R;

public final class DrawableState
{
    public interface StateHolderProvider
    {
        public StateHolder[] getStateHolders();
    }

    public static int[] getAdditionalState(StateHolderProvider obj)
    {
        if (obj == null) return new int[0];
        final StateHolder[] holders = obj.getStateHolders();
        if (holders == null) return new int[0];
        final int holdersLength = holders.length;
        final int[] states = new int[holdersLength];
        int j = 0;
        for (StateHolder holder : holders) if (holder.isState()) states[j++] = holder.getStateAttribute();
        if (j == holdersLength) return states;
        final int[] statesShort = new int[j];
        System.arraycopy(states, 0, statesShort, 0, j);
        return statesShort;
    }

    public static class StateHolder
    {
        private final int     stateAttribute;
        private final View    view;
        private       boolean state;

        public StateHolder(int stateAttribute, View view)
        {
            this.stateAttribute = stateAttribute;
            this.view = view;
        }

        public int getStateAttribute()
        {
            return stateAttribute;
        }

        public View getView()
        {
            return view;
        }

        public boolean isState()
        {
            return state;
        }

        public void toggleState(boolean refreshDrawableState)
        {
            setState(!state, refreshDrawableState);
        }

        public void toggleState()
        {
            toggleState(true);
        }

        public void setState(boolean state, boolean refreshDrawableState)
        {
            if (this.state != state)
            {
                this.state = state;
                if (refreshDrawableState)
                {
                    final View view = getView();
                    if (view != null) view.refreshDrawableState();
                }
            }
        }

        public void setState(boolean state)
        {
            setState(state, true);
        }
    }

    public static final class StateErrorHolder extends StateHolder
    {
        public static final int STATE_ERROR = R.attr.state_error;

        public StateErrorHolder(View view)
        {
            super(STATE_ERROR, view);
        }

        public interface StateError
        {
            public boolean isError();
            public void setError(boolean error);
            public void toggleError();
        }
    }

    public static final class StateWorkingHolder extends StateHolder
    {
        public static final int STATE_WORKING = R.attr.state_working;

        public StateWorkingHolder(View view)
        {
            super(STATE_WORKING, view);
        }

        public interface StateWorking
        {
            public boolean isWorking();
            public void setWorking(boolean working);
            public void toggleWorking();
        }
    }

    public static final class StateCheckedHolder extends StateHolder
    {
        public static final int STATE_CHECKED = android.R.attr.state_checked;

        public StateCheckedHolder(View view)
        {
            super(STATE_CHECKED, view);
        }
    }

    //public static class WeakStateHolder extends StateHolder
    //{
    //    private final WeakReference<View> viewWeakReference;
    //
    //    public WeakStateHolder(int stateAttribute, View view)
    //    {
    //        super(stateAttribute, null);
    //        viewWeakReference = view == null ? null : new WeakReference<>(view);
    //    }
    //
    //    @Override
    //    public View getView()
    //    {
    //        return viewWeakReference == null ? null : viewWeakReference.get();
    //    }
    //}
}
