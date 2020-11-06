package org.qtproject.qdic;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

	private SharedPreferences prefs;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(listener);
		int theme = Integer.parseInt(prefs.getString("pref_theme", "1"));
		int DARK = 1;
		if (theme == DARK)
			setTheme(R.style.AppBaseTheme);
		else
			setTheme(R.style.AppBaseLightTheme);
		super.onCreate(savedInstanceState);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new mFragment()).commit();
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
					editor.apply();
					Toast.makeText(SettingsActivity.this, "List template empty!", Toast.LENGTH_LONG).show();
					finish();
					startActivity(getIntent());
				}
				if (Utils.containsIllegalChars(sharedPreferences.getString("pref_listTemplate", "list.txt"))) {
					Editor editor = sharedPreferences.edit();
					editor.putString("pref_listTemplate", "list.txt");
					editor.apply();
					Toast.makeText(SettingsActivity.this, getText(R.string.list_bad_chars).toString()  + " * \\ / \" : ? | < >", Toast.LENGTH_LONG).show();
					finish();
					startActivity(getIntent());
				}
			}
		}
	};

	public static class mFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.prefs, rootKey);
		}
	}
}
