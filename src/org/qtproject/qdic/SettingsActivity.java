package org.qtproject.qdic;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
	
	private final int DARK = 1;
	private SharedPreferences prefs;
    
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(listener);
		int theme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		if (theme == DARK)
			setTheme(R.style.AppBaseTheme);
		else
			setTheme(R.style.AppBaseLightTheme);
		super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < 11) {
        	addPreferencesFromResource(R.xml.prefs);
        } else {
        	getFragmentManager().beginTransaction().replace(android.R.id.content, new Fragment()).commit();
        }
    }
	
	@Override
	public void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(listener);
		super.onDestroy();
	}
	
	OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
			if (key.equals("pref_listTemplate")) {
				if (sharedPreferences.getString("pref_listTemplate", "list.txt").isEmpty()) {
					Editor editor = sharedPreferences.edit();
					editor.putString("pref_listTemplate", "list.txt");
					editor.commit();
					Toast.makeText(SettingsActivity.this, "List template empty!", Toast.LENGTH_LONG).show();
					finish();
					startActivity(getIntent());
				}
				if (Utils.cantainsIllegalChars(sharedPreferences.getString("pref_listTemplate", "list.txt"))) {
					Editor editor = sharedPreferences.edit();
					editor.putString("pref_listTemplate", "list.txt");
					editor.commit();
					Toast.makeText(SettingsActivity.this, getText(R.string.list_bad_chars).toString()  + " * \\ / \" : ? | < >", Toast.LENGTH_LONG).show();
					finish();
					startActivity(getIntent());
				}
			}
		}
	};
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class Fragment extends PreferenceFragment {
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs);
		}
	}
}
