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
import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Shell;
import me.eugeniomarletti.tetheringfixer.android.Application;
import me.eugeniomarletti.tetheringfixer.android.PackageManager;
import me.eugeniomarletti.tetheringfixer.command.CommandException;
import me.eugeniomarletti.tetheringfixer.command.CommandNotFoundException;
import me.eugeniomarletti.tetheringfixer.command.CommandResult;
import me.eugeniomarletti.tetheringfixer.command.WaitCommand;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static me.eugeniomarletti.tetheringfixer.Async.ResultRunnable;

public final class Fixer
{
    // http://ipset.netfilter.org/iptables.man.html

    private static final String IPTABLES_CMD      = "iptables";
    private static final String TETHERING_FIX_CMD = IPTABLES_CMD +
            " -t nat -%s natctrl_nat_POSTROUTING -s 192.168.0.0/16 -o rmnet0 -j MASQUERADE";

    private static final String IPTABLES_CMD_CHECK  = "C";
    private static final String IPTABLES_CMD_APPEND = "A"; // tail of chain
    private static final String IPTABLES_CMD_DELETE = "D";

    private static final int RETURN_CODE_CHECK_POSITIVE = 0;

    private static volatile ExecutorService executor = null;

    private static final String TAG = "Fixer";

    private static void log(String message, Throwable error)
    {
        if (Application.isDebug()) Log.d(TAG, message, error);
    }

    private static void log(String message)
    {
        log(message, null);
    }

    public static void shutdown()
    {
        log("Shutting down");
        closeAllShellsAsync(new Async.Callback<Void>()
        {
            @Override
            public void callback(Void result, boolean success, Throwable error)
            {
                if (success) log("Shell closed");
                else log("Error closing shells.", error);
            }
        });
        if (executor != null && !executor.isShutdown())
        {
            executor.shutdown();
            executor = null;
        }
    }

    private static String getTetheringCheckRawCmd()
    {
        return String.format(TETHERING_FIX_CMD, IPTABLES_CMD_CHECK);
    }

    private static String getTetheringAppendRawCmd()
    {
        return String.format(TETHERING_FIX_CMD, IPTABLES_CMD_APPEND);
    }

    private static String getTetheringDeleteRawCmd()
    {
        return String.format(TETHERING_FIX_CMD, IPTABLES_CMD_DELETE);
    }

    private static CommandResult runCommand(String command) throws IOException, CommandException
    {
        final WaitCommand cmd = new WaitCommand(command);
        final Shell shell = Shell.getOpenShell();
        if (shell == null) throw new NullPointerException("Must start a shell before adding a command.");
        shell.add(cmd);
        final CommandResult result = cmd.waitForFinish();

        CommandNotFoundException.throwIfNotFound(result);
        if (result.terminated) throw new CommandException(result);

        return result;
    }

    private static <T> void runAsync(Async.Callback<T> callback, ResultRunnable<T> runnable, Object... extras)
    {
        if (executor == null || executor.isShutdown())
        {
            executor = new ThreadPoolExecutor(0, 2,
                                              65L, TimeUnit.SECONDS,
                                              new SynchronousQueue<Runnable>());
            log("Created new executor");
        }
        Async.runAsync(executor, false, callback, runnable, extras);
    }

    /* *** CHECK ROOT AVAILABLE *** */

    public static void checkRootAvailableAsync(Async.Callback<Boolean> callback)
    {
        runAsync(callback, ACTION_CHECK_ROOT_AVAILABLE);
    }

    private static final ResultRunnable<Boolean> ACTION_CHECK_ROOT_AVAILABLE = new ResultRunnable<Boolean>()
    {
        @Override
        public Boolean run(Object... extras) throws Throwable
        {
            return checkRootAvailable();
        }
    };

    public static boolean checkRootAvailable()
    {
        return RootTools.isRootAvailable();
    }

    /* *** START ROOT SHELL *** */

    public static void startRootShellAsync(Async.Callback<Void> callback)
    {
        runAsync(callback, ACTION_START_ROOT_SHELL);
    }

    private static final ResultRunnable<Void> ACTION_START_ROOT_SHELL = new ResultRunnable<Void>()
    {
        @Override
        public Void run(Object... extras) throws Throwable
        {
            startRootShell();
            return null;
        }
    };

    public static void startRootShell() throws TimeoutException, RootDeniedException, IOException
    {
        Shell.startRootShell(60000, 3);
    }

    /* *** CLOSE ALL SHELLS *** */

    public static void closeAllShellsAsync(Async.Callback<Void> callback)
    {
        runAsync(callback, ACTION_CLOSE_ALL_SHELLS);
    }

    private static final ResultRunnable<Void> ACTION_CLOSE_ALL_SHELLS = new ResultRunnable<Void>()
    {
        @Override
        public Void run(Object... extras) throws Throwable
        {
            closeAllShells();
            return null;
        }
    };

    public static void closeAllShells() throws IOException
    {
        Shell.closeAll();
    }

    /* *** CHECK IPTABLES EXISTS *** */

    public static void checkIptablesExistsAsync(Async.Callback<Boolean> callback)
    {
        runAsync(callback, ACTION_CHECK_IPTABLES_EXISTS);
    }

    private static final ResultRunnable<Boolean> ACTION_CHECK_IPTABLES_EXISTS = new ResultRunnable<Boolean>()
    {
        @Override
        public Boolean run(Object... extras) throws Throwable
        {
            return checkIptablesExists();
        }
    };

    public static boolean checkIptablesExists() throws IOException, CommandException
    {
        final CommandResult result = runCommand(IPTABLES_CMD);
        return result != null && result.exitcode != 127; // && result.exitcode == 2
    }

    /* *** CHECK FIX *** */

    public static void checkFixAsync(Async.Callback<Boolean> callback)
    {
        runAsync(callback, ACTION_CHECK_FIX);
    }

    private static final ResultRunnable<Boolean> ACTION_CHECK_FIX = new ResultRunnable<Boolean>()
    {
        @Override
        public Boolean run(Object... extras) throws Throwable
        {
            return checkFix();
        }
    };

    public static boolean checkFix() throws CommandException, IOException
    {
        return runCommand(getTetheringCheckRawCmd()).exitcode == RETURN_CODE_CHECK_POSITIVE;
    }

    /* *** FIX *** */

    public static void fixAsync(Async.Callback<Void> callback, boolean check, final boolean deleteBefore)
    {
        runAsync(callback, ACTION_FIX, check, deleteBefore);
    }

    private static final ResultRunnable<Void> ACTION_FIX = new ResultRunnable<Void>()
    {
        @Override
        public Void run(Object... extras) throws Throwable
        {
            fix((boolean)extras[0], (boolean)extras[1]);
            return null;
        }
    };

    public static void fix(boolean check, final boolean deleteBefore) throws IOException, CommandException
    {
        final boolean isFixed = (check || deleteBefore) && checkFix();
        if (isFixed) return;
        if (deleteBefore) runCommand(getTetheringDeleteRawCmd());
        CommandNotFoundException.throwIfNotFound(runCommand(getTetheringAppendRawCmd()));
    }

    /* *** IS FIX AT BOOT ENABLED *** */

    public static void isFixAtBootEnabledAsync(Async.Callback<Boolean> callback)
    {
        runAsync(callback, ACTION_IS_FIX_AT_BOOT_ENABLED);
    }

    private static final ResultRunnable<Boolean> ACTION_IS_FIX_AT_BOOT_ENABLED = new ResultRunnable<Boolean>()
    {
        @Override
        public Boolean run(Object... extras) throws Throwable
        {
            return isFixAtBootEnabled();
        }
    };

    public static boolean isFixAtBootEnabled()
    {
        final Context context = Application.getInstance();
        final boolean bootReceiver = PackageManager.isBootReceiverEnabled(Application.getInstance());
        final boolean stepsService = PackageManager.isStepsServiceEnabled(Application.getInstance());
        if (stepsService != bootReceiver) // one is disabled
        {
            if (stepsService) PackageManager.setStepsServiceEnabled(context, false);
            if (bootReceiver) PackageManager.setBootReceiverEnabled(context, false);
            return false;
        }
        else return bootReceiver;
    }

    /* *** SET FIX AT BOOT ENABLED *** */

    public static void setFixAtBootEnabledAsync(Async.Callback<Void> callback, boolean fixAtBoot)
    {
        runAsync(callback, ACTION_SET_FIX_AT_BOOT_ENABLED, fixAtBoot);
    }

    private static final ResultRunnable<Void> ACTION_SET_FIX_AT_BOOT_ENABLED = new ResultRunnable<Void>()
    {
        @Override
        public Void run(Object... extras) throws Throwable
        {
            setFixAtBootEnabled((Boolean)extras[0]);
            return null;
        }
    };

    public static void setFixAtBootEnabled(boolean fixAtBoot)
    {
        final Context context = Application.getInstance();
        PackageManager.setBootReceiverEnabled(context, fixAtBoot);
        PackageManager.setStepsServiceEnabled(context, fixAtBoot);
    }
}
