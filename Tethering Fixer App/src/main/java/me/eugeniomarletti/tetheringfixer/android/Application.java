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

import android.util.Log;
import com.stericson.RootTools.RootTools;
import me.eugeniomarletti.tetheringfixer.BuildConfig;
import me.eugeniomarletti.tetheringfixer.Secrets;
import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "")
public final class Application extends android.app.Application
{
    static
    {
        RootTools.debugMode = isDebug();
    }

    private static volatile Application instance = null;

    public static Application getInstance()
    {
        return instance;
    }

    public Application()
    {
        instance = this;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        ACRA.init(this);
        final ACRAConfiguration config = ACRA.getConfig();
        config.setHttpMethod(Secrets.ACRA_HTTP_METHOD);
        config.setReportType(Secrets.ACRA_REPORT_TYPE);
        config.setFormUri(Secrets.ACRA_FORM_URI);
        config.setFormUriBasicAuthLogin(Secrets.ACRA_FORM_URI_BASIC_AUTH_LOGIN);
        config.setFormUriBasicAuthPassword(Secrets.ACRA_FORM_URI_BASIC_AUTH_PASSWORD);
        ACRA.getErrorReporter().setDefaultReportSenders(); // see https://github.com/ACRA/acra/issues/58
        if (isDebug())
        {
            Log.d("Application", "Disabling ACRA in debug mode");
            ACRA.getErrorReporter().setEnabled(false);
        }
    }

    public static boolean isDebug()
    {
        return BuildConfig.DEBUG;
    }
}
