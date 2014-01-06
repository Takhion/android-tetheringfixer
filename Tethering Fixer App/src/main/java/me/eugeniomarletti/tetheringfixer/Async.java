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

import android.os.Handler;

import java.util.concurrent.Executor;

public final class Async
{
    public static interface Callback<T>
    {
        public void callback(T result, boolean success, Throwable error);
    }

    public static interface MainThreadCallback<T> extends Callback<T>
    {
        public void mainThreadCallback(T result, boolean success, Throwable error);
    }

    public static interface ResultRunnable<T>
    {
        public T run(Object... extras) throws Throwable;
    }

    public static abstract class SimpleMainThreadCallback<T> implements MainThreadCallback<T>
    {
        @Override
        public void callback(T result, boolean success, Throwable error)
        {
        }
    }

    public static <T> void runAsync(Handler handler, boolean useHandlerOnlyIfMainThread,
                                    Callback<T> callback, ResultRunnable<T> runnable, Object... runnableExtras)
    {
        try
        {
            if (!useHandlerOnlyIfMainThread || Utils.isMainThread())
                handler.post(new RunnableWrapper<>(callback, runnable, runnableExtras));
            else runnable.run(callback, runnableExtras);
        }
        catch (Throwable e)
        {
            Async.failure(callback, e);
        }
    }

    public static <T> void runAsync(Handler handler,
                                    Callback<T> callback, ResultRunnable<T> runnable, Object... runnableExtras)
    {
        runAsync(handler, false, callback, runnable, runnableExtras);
    }

    public static <T> void runAsync(Executor executor, boolean useExecutorOnlyIfMainThread,
                                    Callback<T> callback, ResultRunnable<T> runnable, Object... runnableExtras)
    {
        try
        {
            if (!useExecutorOnlyIfMainThread || Utils.isMainThread())
                executor.execute(new RunnableWrapper<>(callback, runnable, runnableExtras));
            else runnable.run(callback, runnableExtras);
        }
        catch (Throwable e)
        {
            Async.failure(callback, e);
        }
    }

    public static <T> void runAsync(Executor executor,
                                    Callback<T> callback, ResultRunnable<T> runnable, Object... runnableExtras)
    {
        runAsync(executor, false, callback, runnable, runnableExtras);
    }

    private static <T> void returnCallback(final Callback<T> callback,
                                           final T result, final boolean success, final Throwable error)
    {
        if (callback != null)
        {
            callback.callback(result, success, error);
            if (callback instanceof MainThreadCallback) Utils.runOnMainThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ((MainThreadCallback<T>)callback).mainThreadCallback(result, success, error);
                }
            });
        }
    }

    private static <T> void success(Callback<T> callback, T result, Throwable error)
    {
        returnCallback(callback, result, true, error);
    }

    private static <T> void success(Callback<T> callback, T result)
    {
        success(callback, result, null);
    }

    private static <T> void success(Callback<T> callback)
    {
        success(callback, null, null);
    }

    private static <T> void failure(Callback<T> callback, Throwable error, T result)
    {
        returnCallback(callback, result, false, error);
    }

    private static <T> void failure(Callback<T> callback, Throwable error)
    {
        failure(callback, error, null);
    }

    private static <T> void failure(Callback<T> callback)
    {
        failure(callback, null, null);
    }

    private static class RunnableWrapper<T> implements Runnable
    {
        public final Callback<T>       callback;
        public final ResultRunnable<T> runnable;
        public final Object[]          extras;

        private RunnableWrapper(Callback<T> callback, ResultRunnable<T> runnable, Object... extras)
        {
            this.callback = callback;
            this.runnable = runnable;
            this.extras = extras;
        }

        @Override
        public void run()
        {
            T result;
            try
            {
                result = runnable.run(extras);
            }
            catch (Throwable e)
            {
                Async.failure(callback, e);
                return;
            }
            Async.success(callback, result);
        }
    }

    public static final class CallbackException extends RuntimeException
    {
        public final Object  result;
        public final boolean success;

        public CallbackException(String detailMessage, Object result, boolean success, Throwable throwable)
        {
            super(detailMessage, throwable);
            this.result = result;
            this.success = success;
        }

        public CallbackException(Object result, boolean success, Throwable throwable)
        {
            this(null, result, success, throwable);
        }
    }
}
