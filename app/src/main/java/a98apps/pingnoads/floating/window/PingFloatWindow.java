package a98apps.pingnoads.floating.window;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;
import a98apps.pingnoads.floating.util.PingFloat;
import a98apps.pingnoads.util.SecurityPreferences;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;

public class PingFloatWindow extends StandOutWindow
{
    private ArrayAdapter<Spanned> adapter;
    private PingFloat preparePing;
    private SecurityPreferences mSecurityPreferences;

    @Override
    public String getAppName() {
        return "Ping Tool";
    }

    @Override
    public int getAppIcon() {
        return R.mipmap.ic_launcher;
    }

    @Override
    public void createAndAttachView(final FrameLayout frame) {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ping_float_window, frame, true);

        ListView listaPing = frame.findViewById(R.id.lista);

        adapter = new ArrayAdapter<Spanned>(this, R.layout.custom_listview_float, new ArrayList<Spanned>()){
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                switch (mSecurityPreferences.getSetting(PingConstants.TEXT_SIZE))
                {
                    case PingConstants.TEXT_SMALL:
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_small));
                        break;
                    case PingConstants.TEXT_MID:
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_mid));
                        break;
                        default:
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_big));
                }
                return textView;
            }
        };
        listaPing.setAdapter(adapter);

        setFontSize(mSecurityPreferences.getSetting(PingConstants.TEXT_SIZE), frame);

        preparePing = new PingFloat(getApplicationContext(), getUrl(), mSecurityPreferences, this);
    }

    @Override
    public StandOutLayoutParams getParams(int id)
    {
        mSecurityPreferences = new SecurityPreferences(getApplicationContext());
        switch (mSecurityPreferences.getSetting(PingConstants.TEXT_SIZE))
        {
            case PingConstants.TEXT_SMALL:
                return new StandOutLayoutParams(id, (int) getResources().getDimension(R.dimen.window_small_width), (int) getResources().getDimension(R.dimen.window_small_height), StandOutLayoutParams.RIGHT, StandOutLayoutParams.TOP);
            case PingConstants.TEXT_MID:
                return new StandOutLayoutParams(id, (int) getResources().getDimension(R.dimen.window_mid_width), (int) getResources().getDimension(R.dimen.window_mid_height), StandOutLayoutParams.RIGHT, StandOutLayoutParams.TOP);
            default:
                return new StandOutLayoutParams(id, (int) getResources().getDimension(R.dimen.window_large_width), (int) getResources().getDimension(R.dimen.window_large_height), StandOutLayoutParams.RIGHT, StandOutLayoutParams.TOP);
        }
    }
    @Override
    public int getFlags() {
        return StandOutFlags.FLAG_DECORATION_SYSTEM | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE;
    }

    @Override
    public String getPersistentNotificationMessage()
    {
        return "Click to close the Ping Window";
    }
    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return StandOutWindow.getCloseIntent(this, PingFloatWindow.class, id);
    }

    @Override
    public void onFinishWindow()
    {
        stopPing();
        adapter.clear();
    }
    public void stopPing()
    {
        if(preparePing != null)
        {
            preparePing.interrupt();
            preparePing = null;
        }
    }
    public void addItems(final String text)
    {
        adapter.add(Html.fromHtml(text));
    }
}
