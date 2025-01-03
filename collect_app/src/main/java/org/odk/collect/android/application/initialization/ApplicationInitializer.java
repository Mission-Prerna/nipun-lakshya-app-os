package org.odk.collect.android.application.initialization;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import net.danlew.android.joda.JodaTimeAndroid;

import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.xform.parse.XFormParser;
import org.odk.collect.android.BuildConfig;
import org.odk.collect.android.application.Collect1;
import org.odk.collect.android.geo.MapboxUtils;
import org.odk.collect.android.logic.PropertyManager;
import org.odk.collect.android.logic.actions.setgeopoint.CollectSetGeopointActionHandler;
import org.odk.collect.android.preferences.AdminSharedPreferences;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.utilities.LocaleHelper;
import org.odk.collect.android.utilities.NotificationUtils;
import org.odk.collect.utilities.UserAgentProvider;

import java.util.Locale;

import timber.log.Timber;

public class ApplicationInitializer {

    private final Application context;
    private final UserAgentProvider userAgentProvider;
    private final SettingsPreferenceMigrator preferenceMigrator;
    private final PropertyManager propertyManager;
    private final GeneralSharedPreferences generalSharedPreferences;
    private final AdminSharedPreferences adminSharedPreferences;

    public ApplicationInitializer(Application context, UserAgentProvider userAgentProvider, SettingsPreferenceMigrator preferenceMigrator, PropertyManager propertyManager) {
        this.context = context;
        this.userAgentProvider = userAgentProvider;
        this.preferenceMigrator = preferenceMigrator;
        this.propertyManager = propertyManager;

        generalSharedPreferences = GeneralSharedPreferences.getInstance();
        adminSharedPreferences = AdminSharedPreferences.getInstance();
    }

    public void initialize() {
        initializePreferences();
        initializeFrameworks();
        initializeLocale();
    }

    public void initializePreferences() {
        performMigrations();
        reloadSharedPreferences();
    }

    public void initializeFrameworks() {
        NotificationUtils.createNotificationChannel(context);
        JodaTimeAndroid.init(context);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        initializeMapFrameworks();
        initializeJavaRosa();
    }

    public void initializeLocale() {
        Collect1.defaultSysLanguage = Locale.getDefault().getLanguage();
        new LocaleHelper().updateLocale(context);
    }

    private void initializeJavaRosa() {
        propertyManager.reload();
        org.javarosa.core.services.PropertyManager
                .setPropertyManager(propertyManager);

        // Register prototypes for classes that FormDef uses
        PrototypeManager.registerPrototypes(JavaRosaCoreModule.classNames);
        PrototypeManager.registerPrototypes(CoreModelModule.classNames);
        new XFormsModule().registerModule();

        // When registering prototypes from Collect, a proguard exception also needs to be added
        PrototypeManager.registerPrototype("org.odk.collect.android.logic.actions.setgeopoint.CollectSetGeopointAction");
        XFormParser.registerActionHandler(CollectSetGeopointActionHandler.ELEMENT_NAME, new CollectSetGeopointActionHandler());
    }

    private void reloadSharedPreferences() {
        generalSharedPreferences.reloadPreferences();
        adminSharedPreferences.reloadPreferences();
    }

    private void performMigrations() {
        preferenceMigrator.migrate(generalSharedPreferences.getSharedPreferences(), adminSharedPreferences.getSharedPreferences());
    }

    private void initializeMapFrameworks() {
        try {
            Handler handler = new Handler(context.getMainLooper());
            handler.post(() -> {
                // This has to happen on the main thread but we might call `initialize` from tests
                new com.google.android.gms.maps.MapView(context).onCreate(null);
            });
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgentProvider.getUserAgent());
            MapboxUtils.initMapbox();
        } catch (Exception | Error ignore) {
            // ignored
        }
    }

    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return;
            }

//            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
//            crashlytics.log((priority == Log.ERROR ? "E/" : "W/") + tag + ": " + message);
//
//            if (t != null && priority == Log.ERROR) {
//                crashlytics.recordException(t);
//            }
        }
    }
}

