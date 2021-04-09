package a98apps.pingnoads.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;

public class SecurityPreferences{
    private final SharedPreferences mSharedPreferences;

    public SecurityPreferences(Context context)
    {
        this.mSharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
    }
    public void saveArray(List value)
    {
        this.mSharedPreferences.edit().putInt(PingConstants.ADDRESS_RECENT, value.size()).apply();
        for(int i=0;i<value.size();i++)
        {
            this.mSharedPreferences.edit().remove(PingConstants.ADDRESS_INDEX + i).apply();
            this.mSharedPreferences.edit().putString(PingConstants.ADDRESS_INDEX + i, value.get(i).toString()).apply();
        }
    }
    public List<String> getArray()
    {
        List<String> addressRecent = new ArrayList<>();
        for(int i=0;i<this.mSharedPreferences.getInt(PingConstants.ADDRESS_RECENT, 0);i++)
        {
            addressRecent.add(this.mSharedPreferences.getString(PingConstants.ADDRESS_INDEX + i, null));
        }
        return addressRecent;
    }
    public void saveSetting(String key, String value)
    {
        this.mSharedPreferences.edit().putString(key, value).apply();
    }
    public String getSetting(String key)
    {
        return this.mSharedPreferences.getString(key,"");
    }
    public int getSpinner(int id)
    {
        if(id == PingConstants.SPINNER_INTERVAL) {
            String spinner = mSharedPreferences.getString(PingConstants.INTERVAL, "");
            assert spinner != null;
            switch (spinner) {
                case PingConstants.VERYLONG:
                    return 0;
                case PingConstants.LONG:
                    return 1;
                case PingConstants.NORMAL:
                    return 2;
                case PingConstants.SHORT:
                    return 3;
                case PingConstants.VERYSHORT:
                    return 4;
                default:
                    return PingConstants.CUSTOM;
            }
        }
        else if(id == PingConstants.SPINNER_COUNT)
        {
            String spinner = mSharedPreferences.getString(PingConstants.COUNT_PING, "");
            assert spinner != null;
            switch (spinner) {
                case PingConstants.COUNT_UNLIMITED:
                    return 0;
                case PingConstants.COUNT_10:
                    return 1;
                case PingConstants.COUNT_100:
                    return 2;
                case PingConstants.COUNT_500:
                    return 3;
                case PingConstants.COUNT_1000:
                    return 4;
                case PingConstants.COUNT_2000:
                    return 5;
                default:
                    return PingConstants.COUNT_CUSTOM;
            }
        }
        else if(id == PingConstants.SPINNER_MODE)
        {
            String spinner = mSharedPreferences.getString(PingConstants.PING_MODE, "");
            assert spinner != null;
            switch (spinner) {
                case PingConstants.IPV4_ONLY:
                    return 0;
                case PingConstants.IPV4_IPV6:
                    return 1;
                default:
                    return -1;
            }
        }
        else
        {
            String spinner = mSharedPreferences.getString(PingConstants.TEXT_SIZE, "");
            assert spinner != null;
            switch (spinner) {
                case PingConstants.TEXT_SMALL:
                    return 0;
                case PingConstants.TEXT_MID:
                    return 1;
                default:
                    return 2;
            }
        }
    }
    public String convertSpinner(int position, int id)
    {
        if(id == PingConstants.SPINNER_INTERVAL)
        {
            switch (position)
            {
                case 0:
                    return PingConstants.VERYLONG;
                case 1:
                    return PingConstants.LONG;
                case 2:
                    return PingConstants.NORMAL;
                case 3:
                    return PingConstants.SHORT;
                default:
                    return PingConstants.VERYSHORT;
            }
        }
        else if(id == PingConstants.SPINNER_COUNT)
        {
            switch (position)
            {
                case 0:
                    return PingConstants.COUNT_UNLIMITED;
                case 1:
                    return PingConstants.COUNT_10;
                case 2:
                    return PingConstants.COUNT_100;
                case 3:
                    return PingConstants.COUNT_500;
                case 4:
                    return PingConstants.COUNT_1000;
                default:
                    return PingConstants.COUNT_2000;
            }
        }
        else if(id == PingConstants.SPINNER_MODE)
        {
            if (position == 0) {
                return PingConstants.IPV4_ONLY;
            }
            return PingConstants.IPV4_IPV6;
        }
        else
        {
            switch (position)
            {
                case 0:
                    return PingConstants.TEXT_SMALL;
                case 1:
                    return PingConstants.TEXT_MID;
                default:
                    return PingConstants.TEXT_BIG;
            }
        }
    }

    public String getSpinnerText(int position, Context context)
    {
        String [] interval = context.getResources().getStringArray(R.array.velocity_array);
        return interval[position];
    }
    public void checkExist()
    {
        SharedPreferences.Editor ed;
        if(!mSharedPreferences.contains(PingConstants.INITIALIZED)){
            ed = mSharedPreferences.edit();
            ed.putBoolean(PingConstants.INITIALIZED, true);
            ed.putString(PingConstants.ENABLE_PACKETLOSS, PingConstants.YES);
            ed.putString(PingConstants.ENABLE_PINGONLY, PingConstants.YES);
            ed.putString(PingConstants.ENABLE_FLOATMODE, PingConstants.NO);
            ed.putString(PingConstants.INTERVAL, PingConstants.NORMAL);
            ed.putString(PingConstants.COUNT_PING, PingConstants.COUNT_UNLIMITED);
            ed.putString(PingConstants.PING_MODE, PingConstants.IPV4_ONLY);
            ed.putString(PingConstants.PACKET_BYTES, PingConstants.DEFAULT_BYTES);
            ed.putString(PingConstants.TEXT_SIZE, PingConstants.TEXT_SMALL);
            ArrayList<String> addressDefault = new ArrayList<>();
            addressDefault.add("8.8.8.8");
            addressDefault.add("8.8.4.4");
            addressDefault.add("1.1.1.1");
            addressDefault.add("4.2.2.4");
            addressDefault.add("9.9.9.9");
            saveArray(addressDefault);
            ed.apply();
        }
        else
        {
            if(!mSharedPreferences.contains(PingConstants.ENABLE_PACKETLOSS)) {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.ENABLE_PACKETLOSS, PingConstants.YES);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.ENABLE_PINGONLY)) {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.ENABLE_PINGONLY, PingConstants.YES);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.ENABLE_FLOATMODE))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.ENABLE_FLOATMODE, PingConstants.NO);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.INTERVAL))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.INTERVAL, PingConstants.NORMAL);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.COUNT_PING))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.COUNT_PING, PingConstants.COUNT_UNLIMITED);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.PING_MODE))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.PING_MODE, PingConstants.IPV4_ONLY);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.TEXT_SIZE))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.TEXT_SIZE, PingConstants.TEXT_SMALL);
                ed.apply();
            }

            if(!mSharedPreferences.contains(PingConstants.PACKET_BYTES))
            {
                ed = mSharedPreferences.edit();
                ed.putString(PingConstants.PACKET_BYTES, PingConstants.DEFAULT_BYTES);
                ed.apply();
            }
        }
    }
    public void resetToDefault()
    {
        this.mSharedPreferences.edit().clear().apply();
        checkExist();
    }
}
