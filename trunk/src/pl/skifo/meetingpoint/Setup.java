package pl.skifo.meetingpoint;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

public class Setup extends PreferenceActivity {
    
    public static final String PREF_LONLAT_FORMAT_SAVE = "latlon_format_save";
    public static final String PREF_INCLUDE_LINK_SAVE = "include_link_save";
    
    public static final String PREF_LONLAT_FORMAT_SHARE = "latlon_format_share";
    public static final String PREF_INCLUDE_LINK_SHARE = "include_link_share";
    
    public static final String FORMAT_DEGREES = "DDD.DDDDD";
    public static final String FORMAT_MINUTES = "DDD:MM.MMMMM";
    public static final String FORMAT_SECONDS = "DDD:MM:SS.SSSSS";
    

    public static final String ONLY_LL = "only_ll"; // key0
    public static final String ONLY_LINK = "only_link"; // key1
    public static final String LL_AND_LINK = "ll_and_link"; // key2
    
    

    //private static final String PREFIX = "Preferred lat/lon format: ";
    
    private int pref2resId(CharSequence prefValue) {
        if (LL_AND_LINK.equals(prefValue))
            return R.string.include_link_key2;
        if (ONLY_LL.equals(prefValue))
            return R.string.include_link_key0;
        if (ONLY_LINK.equals(prefValue))
            return R.string.include_link_key1;
        return R.string.include_link_key2;
    }
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setup);

        String latlonFormat = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_LONLAT_FORMAT_SAVE, FORMAT_MINUTES);
        final ListPreference p = (ListPreference) findPreference(PREF_LONLAT_FORMAT_SAVE);
        p.setSummary(latlonFormat);
        p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((ListPreference)preference).setSummary((CharSequence) newValue);
                return true;
            }
        });

        latlonFormat = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_LONLAT_FORMAT_SHARE, FORMAT_DEGREES);
        final ListPreference p1 = (ListPreference) findPreference(PREF_LONLAT_FORMAT_SHARE);
        p1.setSummary(latlonFormat);
        p1.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((ListPreference)preference).setSummary((CharSequence) newValue);
                return true;
            }
        });

        String linkSave = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_INCLUDE_LINK_SAVE, LL_AND_LINK);
        final ListPreference p3 = (ListPreference) findPreference(PREF_INCLUDE_LINK_SAVE);
        p3.setSummary(pref2resId(linkSave));
        p3.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int id = pref2resId((CharSequence) newValue);
                ((ListPreference)preference).setSummary(id);
                return true;
            }
        });
        
        
        String linkShare = PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_INCLUDE_LINK_SHARE, LL_AND_LINK);
        final ListPreference p2 = (ListPreference) findPreference(PREF_INCLUDE_LINK_SHARE);
        p2.setSummary(pref2resId(linkShare));
        p2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int id = pref2resId((CharSequence) newValue);
                ((ListPreference)preference).setSummary(id);
                return true;
            }
        });
        
    }
}
