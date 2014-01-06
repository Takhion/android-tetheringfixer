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

import android.content.ComponentName;
import android.content.Context;

public final class PackageManager
{
    private static final boolean DEFAULT_BOOT_RECEIVER_ENABLED = true;
    private static final boolean DEFAULT_STEPS_SERVICE_ENABLED = true;

    @SuppressWarnings("ConstantConditions")
    public static boolean isBootReceiverEnabled(Context context)
    {
        final int enabled =
                context.getPackageManager().getComponentEnabledSetting(getBootReceiverComponentName(context));
        return enabled == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || enabled == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    @SuppressWarnings("ConstantConditions")
    public static void setBootReceiverEnabled(Context context, boolean enabled)
    {
        context.getPackageManager().setComponentEnabledSetting(
                getBootReceiverComponentName(context),
                enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                android.content.pm.PackageManager.DONT_KILL_APP);
    }

    private static ComponentName bootReceiverComponentName = null;

    private static ComponentName getBootReceiverComponentName(Context context)
    {
        if (bootReceiverComponentName == null)
            bootReceiverComponentName = new ComponentName(context, BootBroadcastReceiver.class);
        return bootReceiverComponentName;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isStepsServiceEnabled(Context context)
    {
        final int enabled =
                context.getPackageManager().getComponentEnabledSetting(getStepsServiceComponentName(context));
        return enabled == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || enabled == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    @SuppressWarnings("ConstantConditions")
    public static void setStepsServiceEnabled(Context context, boolean enabled)
    {
        context.getPackageManager().setComponentEnabledSetting(
                getStepsServiceComponentName(context),
                enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                android.content.pm.PackageManager.DONT_KILL_APP);
    }

    private static ComponentName stepsServiceComponentName = null;

    private static ComponentName getStepsServiceComponentName(Context context)
    {
        if (stepsServiceComponentName == null)
            stepsServiceComponentName = new ComponentName(context, StepsService.class);
        return stepsServiceComponentName;
    }

}
