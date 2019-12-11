package com.bmxgates.logger;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

	public static String WHEEL_SIZE = "535";

	public static String SPLITS = "1524,3048,4572,9144";

	public static String RUNUP = "18288";
	
	public static String SPRINT = "48768";
	
	public static String BOX = "33516";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}
		
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.pref_headers, target);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference, String defaultValue) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), defaultValue));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			bindPreferenceSummaryToValue(findPreference("wheel_size"), WHEEL_SIZE);
			bindPreferenceSummaryToValue(findPreference("splits"), SPLITS);
			bindPreferenceSummaryToValue(findPreference("runup"), RUNUP);
			bindPreferenceSummaryToValue(findPreference("sprint"), SPRINT);
			bindPreferenceSummaryToValue(findPreference("box"), BOX);
		}
	}

	/**
	 * See: http://securityintelligence.com/new-vulnerability-android-framework-fragment-injection/#
	 * 
	 * Implementation should be fixed
	 * 
	 * @param context
	 * @return
	 */
	@Override
	public boolean isValidFragment(String name){
		return true;
	}
	
	public static long[] getSplits(Context context) {
		String[] preferences = PreferenceManager.getDefaultSharedPreferences(context).getString("splits", SPLITS).split(",");
		long [] splits = new long[preferences.length];
		for (int i=0;i<preferences.length;i++){
			splits[i]=Long.parseLong(preferences[i]);
		}
		
		return splits;
}

	public static int getWheelSize(Context context) {
		try {
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("wheel_size", WHEEL_SIZE));
		} catch (NumberFormatException e) {
			return Integer.parseInt(WHEEL_SIZE);
		}
	}

	public static boolean getAutoStop(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("auto_stop", false);
	}

	public static long getRunupDistance(Context context) {
		try {
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("runup", RUNUP));
		} catch (NumberFormatException e) {
			return Integer.parseInt(RUNUP);
		}
	}

	public static long getSprintDistance(Context context) {
		try {
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("sprint", SPRINT));
		} catch (NumberFormatException e) {
			return Integer.parseInt(SPRINT);
		}
	}

	public static long getBoxSprintDistance(Context context) {
		try {
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("box", BOX));
		} catch (NumberFormatException e) {
			return Integer.parseInt(BOX);
		}
	}

}
