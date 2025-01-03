package org.odk.collect.android.formentry.backgroundlocation;

import android.location.Location;


import org.odk.collect.android.application.Collect1;
import org.odk.collect.android.formentry.audit.AuditConfig;
import org.odk.collect.android.formentry.audit.AuditEvent;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.utilities.PermissionUtils;
import org.odk.collect.android.utilities.PlayServicesChecker;

import static org.odk.collect.android.preferences.GeneralKeys.KEY_BACKGROUND_LOCATION;

/**
 * Wrapper on resources needed by {@link BackgroundLocationManager} to make testing easier.
 *
 * Ideally this would be replaced by more coherent abstractions in the future.
 *
 * The methods on the {@link FormController} are wrapped here rather
 * than the form controller being passed in when constructing the {@link BackgroundLocationManager}
 * because the form controller isn't set until
 * {@link org.odk.collect.android.activities.FormEntryActivity}'s onCreate.
 */
public class BackgroundLocationHelper {
    boolean isAndroidLocationPermissionGranted() {
        return PermissionUtils.areLocationPermissionsGranted(Collect1.getInstance().getAppContext());
    }

    boolean isBackgroundLocationPreferenceEnabled() {
        return GeneralSharedPreferences.getInstance().getBoolean(KEY_BACKGROUND_LOCATION, true);
    }

    boolean arePlayServicesAvailable() {
        return new PlayServicesChecker().isGooglePlayServicesAvailable(Collect1.getInstance().getAppContext());
    }

    /**
     * @return true if the global form controller has been initialized.
     */
    boolean isCurrentFormSet() {
        return Collect1.getInstance().getFormController() != null;
    }

    /**
     * @return true if the current form definition requests any kind of background location.
     *
     * Precondition: the global form controller has been initialized.
     */
    boolean currentFormCollectsBackgroundLocation() {
        return Collect1.getInstance().getFormController().currentFormCollectsBackgroundLocation();
    }

    /**
     * @return true if the current form definition requests a background location audit, false
     * otherwise.
     *
     * Precondition: the global form controller has been initialized.
     */
    boolean currentFormAuditsLocation() {
        return Collect1.getInstance().getFormController().currentFormAuditsLocation();
    }

    /**
     * @return the configuration for the audit requested by the current form definition.
     *
     * Precondition: the global form controller has been initialized.
     */
    AuditConfig getCurrentFormAuditConfig() {
        return Collect1.getInstance().getFormController().getSubmissionMetadata().auditConfig;
    }

    /**
     * Logs an audit event of the given type.
     *
     * Precondition: the global form controller has been initialized.
     */
    void logAuditEvent(AuditEvent.AuditEventType eventType) {
        Collect1.getInstance().getFormController().getAuditEventLogger().logEvent(eventType, false, System.currentTimeMillis());
    }

    /**
     * Provides the location to the global audit event logger.
     *
     * Precondition: the global form controller has been initialized.
     */
    void provideLocationToAuditLogger(Location location) {
        Collect1.getInstance().getFormController().getAuditEventLogger().addLocation(location);
    }
}
