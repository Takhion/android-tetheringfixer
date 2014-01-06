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

package me.eugeniomarletti.tetheringfixer.command;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

// don't make final
public class WaitCommand extends Command
{
    private static final int LOG_TYPE_DEBUG = 3;

    private static int lastId = -1;

    private static final Field timeout;

    public final int id;
    private       CommandResult  result    = null;
    private final StringBuilder  output    = new StringBuilder();
    private final CountDownLatch countdown = new CountDownLatch(1);

    static
    {
        final String timeoutFieldName = "timeout";
        Field _timeout;
        try
        {
            _timeout = Command.class.getDeclaredField(timeoutFieldName);
            _timeout.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            _timeout = null;
            RootTools.log(String.format("WARNING! Field '%s' not found in super class.", timeoutFieldName),
                          LOG_TYPE_DEBUG, e);
        }
        timeout = _timeout;
    }

    public int getTimeout()
    {
        if (timeout != null)
        {
            try
            {
                return timeout.getInt(this);
            }
            catch (IllegalAccessException ignore)
            {
            }
        }
        return RootTools.default_Command_Timeout;
    }

    public void setTimeout(int timeout)
    {
        if (WaitCommand.timeout != null)
        {
            try
            {
                WaitCommand.timeout.setInt(this, timeout);
            }
            catch (IllegalAccessException ignore)
            {
            }
        }
    }

    private static synchronized int getNextId()
    {
        if (lastId == Integer.MAX_VALUE) lastId = -1;
        return ++lastId;
    }

    public WaitCommand(int id, String... command)
    {
        super(id, false, command);
        this.id = id;
    }

    public WaitCommand(String... command)
    {
        this(getNextId(), command);
    }

    public String getOutput()
    {
        return output.toString();
    }

    @Override
    public void commandOutput(int id, String s)
    {
        output.append(s).append('\n');
    }

    @Override
    protected void startExecution()
    {
        if (isFinished()) throw new RuntimeException(String.format("Can't start finished command (id: %d).", id));
        super.startExecution();
    }

    public CommandResult getResult()
    {
        return result;
    }

    protected void commandCompletedOrTerminated(int id, int exitcode, boolean terminated, String terminatedReason)
    {
        RootTools.log(String.format("Output (id:%d exitcode:%d):\n%s", id, exitcode, getOutput()));
        result = new CommandResult(id, exitcode, terminated, terminatedReason);
        countdown.countDown();
    }

    @Override
    public void commandTerminated(int id, String reason)
    {
        commandCompletedOrTerminated(id, getExitCode(), true, reason);
    }

    @Override
    public void commandCompleted(int id, int exitcode)
    {
        commandCompletedOrTerminated(id, exitcode, false, null);
    }

    public CommandResult waitForFinish()
    {
        RootTools.log(String.format("Waiting for command %d...", id));
        do
        {
            try
            {
                countdown.await();
            }
            catch (InterruptedException e)
            {
                RootTools.log(String.format("...InterruptedException while waiting for command %d...", id),
                              LOG_TYPE_DEBUG, e);
            }
        }
        while (countdown.getCount() > 0);
        RootTools.log(String.format("...Done waiting for command %d", id));

        return getResult();
    }
}
