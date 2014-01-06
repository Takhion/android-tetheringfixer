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
import android.util.Log;
import com.stericson.RootTools.exceptions.RootDeniedException;
import me.eugeniomarletti.tetheringfixer.android.Application;
import org.acra.ACRA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeoutException;

public final class Steps
{
    private static final String TAG = "Steps";

    private static final int ACTION_NONE    = 0;
    private static final int ACTION_START   = 1;
    private static final int ACTION_RETRY   = 2;
    private static final int ACTION_ADVANCE = 3;
    private static final int ACTION_SUCCESS = 4;
    private static final int ACTION_ERROR   = 5;

    private static int pendingAction;
    private static final Runnable pendingActionWrapper = new Runnable()
    {
        @Override
        public void run()
        {
            if (pendingAction != ACTION_NONE)
            {
                final int action = pendingAction;
                pendingAction = ACTION_NONE;
                executeAction(action);
            }
        }
    };

    private static final WeakHashMap<StepListListener, Void> LISTENERS = new WeakHashMap<>(2);

    private static final int PAUSE_BETWEEN_ACTIONS = 1000;

    private static final Handler HANDLER = Utils.MAIN_THREAD_HANDLER;

    private static boolean actionsDelayed;
    private static boolean isError;
    private static boolean isSuccess;
    private static boolean isStarted;
    private static boolean isPaused;

    private static String errorText;

    private static final Step[]       STEPS;
    private static final List<String> LABELS;
    private static int CURRENT_STEP_INDEX = -1;

    static
    {
        STEPS = new Step[]
                {
                        new Step(R.string.step_check_root, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                log("Step 1: check root available...");
                                Fixer.checkRootAvailableAsync(new Async.SimpleMainThreadCallback<Boolean>()
                                {
                                    @Override
                                    public void mainThreadCallback(Boolean result, boolean success, Throwable error)
                                    {
                                        logCallback(result, success, error, true);
                                        final boolean _success = success && result != null;
                                        if (_success && result) advanceStep();
                                        else
                                        {
                                            //error
                                            Integer errorText = null;
                                            if (result != null && !result)
                                                errorText = R.string.error_no_root;
                                            else if (error != null && error instanceof IOException)
                                                errorText = R.string.error_io;
                                            error(errorText);
                                        }
                                    }
                                });
                            }
                        }),

                        new Step(R.string.step_get_root, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                log("Step 2: get root...");
                                Fixer.startRootShellAsync(new Async.SimpleMainThreadCallback<Void>()
                                {
                                    @Override
                                    public void mainThreadCallback(Void result, boolean success, Throwable error)
                                    {
                                        logCallback(result, success, error, false);
                                        if (success) advanceStep();
                                        else
                                        {
                                            // error
                                            Integer errorText = null;
                                            if (error != null)
                                            {
                                                if (error instanceof RootDeniedException)
                                                    errorText = R.string.error_root_denied;
                                                else if (error instanceof TimeoutException)
                                                    errorText = R.string.error_timeout;
                                                else if (error instanceof IOException)
                                                    errorText = R.string.error_io;
                                            }
                                            error(errorText);
                                        }
                                    }
                                });
                            }
                        }),

                        new Step(R.string.step_check_components, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                log("Step 3: check binary exists...");
                                Fixer.checkIptablesExistsAsync(new Async.SimpleMainThreadCallback<Boolean>()
                                {
                                    @Override
                                    public void mainThreadCallback(Boolean result, boolean success, Throwable error)
                                    {
                                        logCallback(result, success, error, true);
                                        final boolean _success = success && result != null;
                                        if (_success) advanceStep();
                                        else
                                        {
                                            //error
                                            Integer errorText = null;
                                            if (result != null && !result)
                                            {
                                                errorText = R.string.error_iptables_not_found;
                                                reportState("iptables_not_found", "true");
                                            }
                                            if (error != null && error instanceof IOException)
                                                errorText = R.string.error_io;
                                            error(errorText);
                                        }
                                    }
                                });
                            }
                        }),

                        new Step(R.string.step_check_fix, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                log("Step 4: check already fixed...");
                                Fixer.checkFixAsync(new Async.SimpleMainThreadCallback<Boolean>()
                                {
                                    @Override
                                    public void mainThreadCallback(Boolean result, boolean success, Throwable error)
                                    {
                                        logCallback(result, success, error, true);
                                        final boolean _success = success && result != null;
                                        if (_success)
                                        {
                                            if (result) success(); // already fixed!
                                            else advanceStep();
                                        }
                                        else
                                        {
                                            //error
                                            Integer errorText = null;
                                            if (error != null && error instanceof IOException)
                                                errorText = R.string.error_io;
                                            error(errorText);
                                        }
                                    }
                                });
                            }
                        }),

                        new Step(R.string.step_apply_fix, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                log("Step 5: apply fix...");
                                Fixer.fixAsync(new Async.SimpleMainThreadCallback<Void>()
                                {
                                    @Override
                                    public void mainThreadCallback(Void result, boolean success, Throwable error)
                                    {
                                        logCallback(result, success, error, false);
                                        if (success) success(); // fixed!
                                        else
                                        {
                                            //error
                                            Integer errorText = null;
                                            if (error != null && error instanceof IOException)
                                                errorText = R.string.error_io;
                                            error(errorText);
                                        }
                                    }
                                }, false, false);
                            }
                        })
                };

        final Application application = Application.getInstance();
        final List<String> _labels = new ArrayList<>(STEPS.length);
        for (Step item : STEPS) _labels.add(application.getString(item.labelId));
        LABELS = Collections.unmodifiableList(_labels);
    }

    private Steps() { }

    public static void pause()
    {
        if (isPaused()) log("Already paused!");
        else
        {
            log("Paused");
            setPaused(true);
            HANDLER.removeCallbacks(pendingActionWrapper);
        }
    }

    public static void resume()
    {
        if (!isPaused()) log("Already resumed!");
        else
        {
            log("Resumed");
            setPaused(false);
            executePendingAction();
        }
    }

    public static void start()
    {
        if (isStarted())
        {
            log("Already started!");
            return;
        }
        log("Start!");
        executeAction(ACTION_START);
    }

    public static void retry()
    {
        if (!isError() && !isSuccess())
        {
            log("Can't retry if still going!");
            return;
        }
        log("Retry!");
        executeAction(ACTION_RETRY);
    }

    public static void startOrRetry()
    {
        if (!isError() && !isSuccess()) start();
        else retry();
    }

    private static void error(final Integer errorText)
    {
        log(String.format("Error! (%d)", getCurrentStep()));
        Steps.setErrorText(errorText == null ? null : Application.getInstance().getString(errorText));
        pendingAction = ACTION_ERROR;
        executePendingAction();
    }

    private static void advanceStep(boolean delay)
    {
        log(String.format("Advancing! delay: %b (%d)", delay, getCurrentStep() + 1));
        pendingAction = ACTION_ADVANCE;
        executePendingAction(delay);
    }

    private static void advanceStep()
    {
        advanceStep(true);
    }

    private static void success()
    {
        log(String.format("Success! (%d)", CURRENT_STEP_INDEX));
        pendingAction = ACTION_SUCCESS;
        executePendingAction();
    }

    private static void executePendingAction(boolean delay)
    {
        if (!isPaused && pendingAction != ACTION_NONE)
        {
            HANDLER.removeCallbacks(pendingActionWrapper);
            if (actionsDelayed && delay /*&& PAUSE_BETWEEN_ACTIONS > 0*/)
                HANDLER.postDelayed(pendingActionWrapper, PAUSE_BETWEEN_ACTIONS);
            else HANDLER.post(pendingActionWrapper);
        }
    }

    private static void executePendingAction()
    {
        executePendingAction(true);
    }

    private static void executeAction(final int action)
    {
        //checkMainThread();

        int step = getCurrentStep();
        reportState("step", Integer.toString(step));
        final String errorText = action == ACTION_ERROR ? getErrorText() : null;

        String actionName;
        switch (action)
        {
            case ACTION_START: actionName = "start"; break;
            case ACTION_RETRY: actionName = "retry"; break;
            case ACTION_ADVANCE: actionName = "advance"; break;
            case ACTION_SUCCESS: actionName = "success"; break;
            case ACTION_ERROR: actionName = "error"; break;
            default: actionName = null;
        }
        reportState("action", actionName);

        switch (action)
        {
            case ACTION_START:
            {
                setStarted(true);
            }
            break;

            case ACTION_RETRY:
            {
                HANDLER.removeCallbacks(pendingActionWrapper);
                setCurrentStep(-1);
                setErrorText(null);
                setError(false);
                setSuccess(false);
                setStarted(false);
            }
            break;

            case ACTION_ADVANCE:
            {
                step = ++CURRENT_STEP_INDEX;
                reportState("step", Integer.toString(step));
                log(String.format("Advancing for real! (%d)", step));
            }
            break;

            case ACTION_SUCCESS:
            {
                log(String.format("Success for real! (%d)", step));
                Fixer.shutdown();
                setSuccess(true);
            }
            break;

            case ACTION_ERROR:
            {
                log(String.format("Error for real! (%d)\n%s", step, errorText));
                setError(true);
            }
            break;

            default: return;
        }

        int i = 0;
        final StepListListener[] listeners = getListeners();
        reportState("listeners", Integer.toString(listeners.length));
        for (StepListListener listener : listeners)
        {
            if (listener == null) // should never happen
            {
                log("Null listener.");
                continue;
            }
            switch (action)
            {
                case ACTION_START: listener.onStepsStart(); break;
                case ACTION_RETRY: listener.onStepsRetry(); break;
                case ACTION_ADVANCE: listener.onAdvanceStep(step); break;
                case ACTION_SUCCESS: listener.onStepsSuccess(step); break;
                case ACTION_ERROR: listener.onStepError(step, errorText); break;
            }
            i++;
        }
        final int listenersNull = listeners.length - i;
        log(String.format("Notified %d listener%s (%d null)", i, i > 1 ? "s" : "", listenersNull));
        reportState("listeners_null", Integer.toString(listenersNull));

        switch (action)
        {
            case ACTION_START:
            {
                advanceStep(false);
            }
            break;

            case ACTION_RETRY:
            {
                start();
            }
            break;

            case ACTION_ADVANCE:
            {
                STEPS[step].run();
            }
            break;
        }
    }

    private static void logCallback(Object result, boolean success, Throwable error, boolean resultExpected)
    {
        reportState("callback_result", result == null ? "NULL" : result.toString());
        reportState("callback_result_expected", Boolean.toString(resultExpected));
        reportState("callback_success", Boolean.toString(success));
        log(String.format("Callback! success: %b\nresult: %s",
                          success, result == null ? null : result.toString()), error);
        if (error != null && !(error instanceof Exception)) throw new Async.CallbackException(result, success, error);
        if ((success && resultExpected && result == null) || (!success && (error == null ||
                !(error instanceof RootDeniedException || error instanceof TimeoutException))))
            reportException(error);
    }

    private static void log(String text, Throwable error)
    {
        if (Application.isDebug()) Log.d(TAG, text, error);
    }

    private static void log(String text)
    {
        log(text, null);
    }

    private static void reportState(String key, String value)
    {
        ACRA.getErrorReporter().putCustomData(key, value);
    }

    private static void reportException(Throwable error)
    {
        ACRA.getErrorReporter().handleException(error);
    }

    private static void checkMainThread()
    {
        if (!Utils.isMainThread()) throw new RuntimeException("Must be called from main Thead.");
    }

    public static int size()
    {
        return STEPS.length;
    }

    public static List<String> getLabels()
    {
        return LABELS;
    }

    public static int getCurrentStep()
    {
        return CURRENT_STEP_INDEX;
    }

    private static void setCurrentStep(int index)
    {
        CURRENT_STEP_INDEX = index;
    }

    private static void setErrorText(String errorText)
    {
        Steps.errorText = errorText;
    }

    public static String getErrorText()
    {
        return errorText;
    }

    public static boolean isActionsDelayed()
    {
        return actionsDelayed;
    }

    public static void setActionsDelayed(boolean actionsDelayed)
    {
        if (Steps.actionsDelayed != actionsDelayed)
        {
            Steps.actionsDelayed = actionsDelayed;
            reportState("actions_delayed", Boolean.toString(actionsDelayed));
            if (!actionsDelayed) executePendingAction(false);
        }
    }

    public static boolean isStarted()
    {
        return isStarted;
    }

    private static void setStarted(boolean isStarted)
    {
        Steps.isStarted = isStarted;
    }

    public static boolean isPaused()
    {
        return isPaused;
    }

    private static void setPaused(boolean isPaused)
    {
        Steps.isPaused = isPaused;
        reportState("is_paused", Boolean.toString(isPaused));
    }

    public static boolean isError()
    {
        return isError;
    }

    private static void setError(boolean isError)
    {
        Steps.isError = isError;
        reportState("is_error", Boolean.toString(isError));
    }

    public static boolean isSuccess()
    {
        return isSuccess;
    }

    private static void setSuccess(boolean isSuccess)
    {
        Steps.isSuccess = isSuccess;
        reportState("is_success", Boolean.toString(isSuccess));
    }

    public static void shutdownIfNoListeners()
    {
        if (LISTENERS.size() == 0) Fixer.shutdown();
    }

    public static void addListener(StepListListener listener)
    {
        if (listener != null && LISTENERS.put(listener, null) != null) log("Added listener.");
    }

    public static void removeListener(StepListListener listener)
    {
        if (listener != null && LISTENERS.remove(listener) != null) log("Removed listener.");
    }

    private static StepListListener[] getListeners()
    {
        return LISTENERS.keySet().toArray(new StepListListener[LISTENERS.size()]);
    }

    public interface StepListListener
    {
        public void onStepsStart();
        public void onStepsRetry();
        public void onStepError(int itemIndex, String errorText);
        public void onAdvanceStep(int itemIndex);
        public void onStepsSuccess(int itemIndex);
    }

    private static class Step implements Runnable
    {
        public final int      labelId;
        public final Runnable action;

        public Step(int labelId, Runnable action)
        {
            this.labelId = labelId;
            this.action = action;
        }

        @Override
        public void run()
        {
            action.run();
        }
    }
}
