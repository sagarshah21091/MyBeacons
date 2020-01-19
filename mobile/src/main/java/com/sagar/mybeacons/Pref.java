package com.sagar.mybeacons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Pref {
    Context context;
    Pref(Context context)
    {
        this.context = context;
    }
    public SharedPreferences getPreferenceManager()
    {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setUUID(String uuid)
    {
        getPreferenceManager().edit().putString("UUID", uuid).commit();
    }

    public String getUUID()
    {
        return getPreferenceManager().getString("UUID",null);
    }
}
