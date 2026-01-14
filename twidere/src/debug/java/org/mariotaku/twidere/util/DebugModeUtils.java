/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.mariotaku.stethoext.bsh.BshRuntimeReplFactoryBuilder;
import org.mariotaku.twidere.util.net.NoIntercept;
import org.mariotaku.twidere.util.stetho.AccountsDumperPlugin;

import okhttp3.OkHttpClient;

/**
 * Created by mariotaku on 15/5/27.
 */
public class DebugModeUtils {

    private DebugModeUtils() {
    }

    public static void initForOkHttpClient(final OkHttpClient.Builder builder) {
        final StethoInterceptor interceptor = new StethoInterceptor();

        builder.addNetworkInterceptor(chain -> {
            if (chain.request().tag() == NoIntercept.INSTANCE) {
                return chain.proceed(chain.request());
            }
            return interceptor.intercept(chain);
        });
    }

    public static void initForApplication(final Application application) {
        if (isMainProcess(application)) {
            Stetho.initialize(Stetho.newInitializerBuilder(application)
                    .enableDumpapp(() -> new Stetho.DefaultDumperPluginsBuilder(application)
                            .provide(new AccountsDumperPlugin(application))
                            .finish())
                    .enableWebKitInspector(() -> new Stetho.DefaultInspectorModulesBuilder(application)
                            .runtimeRepl(new BshRuntimeReplFactoryBuilder(application).build())
                            .finish())
                    .build());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    WebView.setWebContentsDebuggingEnabled(true);
                } catch (RuntimeException e) {
                    // Ignore WebView debugging errors in multi-process scenarios
                    Log.w("DebugModeUtils", "Failed to enable WebView debugging", e);
                }
            }
        }
    }

    private static boolean isMainProcess(final Application application) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            return application.getApplicationInfo().uid == android.os.Process.myUid();
        }
        return application.getPackageName().equals(getProcessName(application));
    }

    private static String getProcessName(final Application application) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            return android.app.Application.getProcessName();
        }
        int pid = android.os.Process.myPid();
        android.app.ActivityManager am = (android.app.ActivityManager) application.getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if (am != null) {
            for (android.app.ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    return processInfo.processName;
                }
            }
        }
        return null;
    }
}
