package a98apps.pingnoads.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import a98apps.pingnoads.BuildConfig;
import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;
import a98apps.pingnoads.floating.window.PingFloatWindow;
import a98apps.pingnoads.util.SecurityPreferences;
import wei.mark.standout.StandOutWindow;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final ViewHolder mViewHolder = new ViewHolder();
    private SecurityPreferences mSecurityPreferences;
    private TabPing tabPing;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);//On ram clear, not save the state, its crashes on restart
        setContentView(R.layout.activity_main);

        mViewHolder.toolbar = findViewById(R.id.toolbar);
        mViewHolder.mViewPager = findViewById(R.id.container);
        mViewHolder.tabLayout = findViewById(R.id.tabs);

        setSupportActionBar(mViewHolder.toolbar);
        mViewHolder.params = (AppBarLayout.LayoutParams) mViewHolder.toolbar.getLayoutParams();
        mViewHolder.params.setScrollFlags(0);

        getSupportActionBar().setIcon(R.mipmap.ic_launcher);

        mViewHolder.mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewHolder.mViewPager.setAdapter(mViewHolder.mSectionsPagerAdapter);

        mViewHolder.tab1 = (TextView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.custom_tab, (ViewGroup) getWindow().getDecorView().getRootView(), false);
        mViewHolder.tab1.setText(getText(R.string.tab_ping));
        mViewHolder.tab1.setTextColor(getResources().getColorStateList(R.drawable.selector_textview));
        mViewHolder.tab1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.selector_icontab1, 0, 0);
        mViewHolder.tabLayout.getTabAt(0).setCustomView(mViewHolder.tab1);

        mViewHolder.tab2 = (TextView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.custom_tab, (ViewGroup) getWindow().getDecorView().getRootView(), false);
        mViewHolder.tab2.setText(getText(R.string.tab_info));
        mViewHolder.tab2.setTextColor(getResources().getColorStateList(R.drawable.selector_textview));
        mViewHolder.tab2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.selector_icontab2, 0, 0);
        mViewHolder.tabLayout.getTabAt(1).setCustomView(mViewHolder.tab2);

        mViewHolder.mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mViewHolder.tabLayout));
        mViewHolder.tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewHolder.mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                if (mViewHolder.inputHost.getError() != null)
                    mViewHolder.inputHost.setError(null);

                tabPing.resetStatusSpinner();
            }
        });


        mSecurityPreferences = new SecurityPreferences(this);
        mSecurityPreferences.checkExist();

        mViewHolder.settingsDialog = new AlertDialog.Builder(this).create();
        mViewHolder.helpDialog = new AlertDialog.Builder(this).create();
        mViewHolder.confirmDialog = new AlertDialog.Builder(this).create();
        mViewHolder.helpItem = new AlertDialog.Builder(MainActivity.this).create();

        new LoadDialogsTask(mViewHolder, this, mSecurityPreferences).execute();

        StandOutWindow.closeAll(this, PingFloatWindow.class);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        state.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mViewHolder.settingsDialog.isShowing())
            mViewHolder.settingsDialog.dismiss();
        if (mViewHolder.helpDialog.isShowing())
            mViewHolder.helpDialog.dismiss();
        if (mViewHolder.helpItem.isShowing())
            mViewHolder.helpItem.dismiss();
        if (mViewHolder.confirmDialog.isShowing())
            mViewHolder.confirmDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            if (mViewHolder.buttonPing.getText().equals(getString(R.string.parar))) {
                mViewHolder.confirmDialog.setTitle(getString(R.string.confirm_dialog));
                mViewHolder.confirmDialog.setMessage(getString(R.string.confirm_dialog_text));
                mViewHolder.confirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.confirm_dialog_positive),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mViewHolder.buttonPing.getText().equals(getString(R.string.parar)))
                                    mViewHolder.buttonPing.performClick();

                                mViewHolder.settingsDialog.show();
                                Button btnPositive = mViewHolder.settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                Button btnNegative = mViewHolder.settingsDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
                                layoutParams.weight = 10;
                                btnNegative.setLayoutParams(layoutParams);
                                btnPositive.setLayoutParams(layoutParams);
                            }
                        });
                mViewHolder.confirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.confirm_dialog_negative),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //--
                            }
                        });
                mViewHolder.confirmDialog.show();
                return true;
            }
            mViewHolder.settingsDialog.show();
            Button btnPositive = mViewHolder.settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnNegative = mViewHolder.settingsDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) btnPositive.getLayoutParams();
            layoutParams.weight = 10;
            btnNegative.setLayoutParams(layoutParams);
            btnPositive.setLayoutParams(layoutParams);
        } else if (id == R.id.action_help) {
            mViewHolder.helpDialog.show();
            Button btnNegative = mViewHolder.helpDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            LinearLayout parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            View leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.action_reportbugs) {
            Intent emailIntent = new Intent(Intent.ACTION_VIEW);
            String subject = "[Bug Report][Ping Tool]";
            String text = getResources().getString(R.string.please_write_details)
                    +"<br><br>--------------------------<br>"
                    + Build.MANUFACTURER.toUpperCase() + "<br>"
                    + Build.MODEL + "<br>Android: "
                    + Build.VERSION.RELEASE + "<br>"
                    + "App Version: " + BuildConfig.VERSION_NAME;

            Uri data = Uri.parse("mailto:98appshelp@gmail.com" + "?subject=" + subject + "&body=" + text);
            emailIntent.setData(data);

            ComponentName emailApp = emailIntent.resolveActivity(this.getPackageManager());
            ComponentName unsupportedAction = ComponentName.unflattenFromString("com.android.fallback/.Fallback");
            if (emailApp != null && !emailApp.equals(unsupportedAction))
            {
                try
                {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
                }
                catch (ActivityNotFoundException i) {
                    i.printStackTrace();
                }
            }
            else
            {
                Toast.makeText(this, getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
            }
        }
        else if(id == R.id.action_rate)
        {
            String url = "https://play.google.com/store/apps/details?id=a98apps.pingnoads";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_reset) {
            mViewHolder.confirmDialog.setTitle(getString(R.string.confirm_dialog));
            mViewHolder.confirmDialog.setMessage(getString(R.string.confirm_dialog_reset));
            mViewHolder.confirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.confirm_dialog_positive),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Toast.makeText(getApplicationContext(), getString(R.string.text_reset), Toast.LENGTH_SHORT).show();
                            mSecurityPreferences.resetToDefault();
                            tabPing.resetRecentSpinner();
                            mViewHolder.spinnerInterval.setSelection(2);//Normal
                            mViewHolder.spinnerCount.setSelection(0);//Ilimitado
                            mViewHolder.spinnerPingMode.setSelection(0);//IPV4 ONLY
                            mViewHolder.spinnerTextSize.setSelection(0);//SMALL
                            mViewHolder.seePacket.setChecked(true);
                            mViewHolder.floatMode.setChecked(false);
                            mViewHolder.pingOnly.setChecked(true);
                            mViewHolder.buttonPing.setText(getString(R.string.ping));
                            mViewHolder.inputBytes.setText(PingConstants.DEFAULT_BYTES);//56
                            if(mViewHolder.spinnerRecent.getItemAtPosition(0) != null)
                                mViewHolder.inputHost.setText(mViewHolder.spinnerRecent.getItemAtPosition(0).toString());

                            mViewHolder.intervalSpeed.setText(mSecurityPreferences.getSpinnerText(mSecurityPreferences.getSpinner(PingConstants.SPINNER_INTERVAL), getApplicationContext()));//Tab Ping

                            if (mViewHolder.packetLayout.getVisibility() != View.VISIBLE)
                                mViewHolder.packetLayout.setVisibility(View.VISIBLE);

                            if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE)
                                mViewHolder.layoutCustomCount.setVisibility(View.GONE);

                            if (mViewHolder.customInterval.getVisibility() == View.VISIBLE)
                                mViewHolder.customInterval.setVisibility(View.GONE);

                            mViewHolder.settingsDialog.dismiss();
                        }
                    });
            mViewHolder.confirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.confirm_dialog_negative),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //--
                        }
                    });
            mViewHolder.confirmDialog.show();
        } else if (id == R.id.button_helppacket || id == R.id.button_helpcount || id == R.id.button_helpinterval
                || id == R.id.button_pingonly || id == R.id.button_helpmode || id == R.id.button_helpbytes
                || id == R.id.button_floatmode || id == R.id.button_text_size) {
            loadHelpItem(id);
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return tabPing = new TabPing().newInstance(mViewHolder, mSecurityPreferences);
                case 1:
                    return new TabInfo().newInstance(mViewHolder);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

    }

    private void loadHelpItem(int id) {
        Button btnNegative;
        LinearLayout parent;
        View leftSpacer;
        if (id == R.id.button_helppacket) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_packet_loss));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.button_helpcount) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_count_host));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.button_helpinterval) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_interval));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.button_pingonly) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_ping_only));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.button_helpmode) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_ping_mode));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        } else if (id == R.id.button_helpbytes) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_packet_bytes));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        }
        else if (id == R.id.button_floatmode) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_floating_mode));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        }
        else if (id == R.id.button_text_size) {
            mViewHolder.helpItem.setMessage(getString(R.string.help_text_size));
            mViewHolder.helpItem.show();
            btnNegative = mViewHolder.helpItem.getButton(AlertDialog.BUTTON_NEGATIVE);
            parent = (LinearLayout) btnNegative.getParent();
            parent.setGravity(Gravity.CENTER_HORIZONTAL);
            leftSpacer = parent.getChildAt(1);
            leftSpacer.setVisibility(View.GONE);
        }
    }

    private static class LoadDialogsTask extends AsyncTask<Void, Void, Void> {
        private LayoutInflater factory;
        private final ViewHolder mViewHolder;
        private final WeakReference<Context> context;
        private final WeakReference<Activity> activity;
        private final WeakReference<SecurityPreferences> mSecurityPreferences;
        private final WeakReference<View.OnClickListener> listener;
        private WeakReference<View> viewSettings;
        private WeakReference<View> viewHelp;

        private LoadDialogsTask(ViewHolder mViewHolder, MainActivity main, SecurityPreferences s) {
            this.mViewHolder = mViewHolder;
            this.context = new WeakReference<>(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH ? main.getApplicationContext() : main);//Fix crash
            this.activity = new WeakReference<>((Activity) main);
            this.mSecurityPreferences = new WeakReference<>(s);
            this.listener = new WeakReference<>((View.OnClickListener) main);
        }

        @Override
        protected void onPreExecute() {

            factory = LayoutInflater.from(context.get());
            viewSettings = new WeakReference<>(factory.inflate(R.layout.dialog_settings, null));
            viewHelp = new WeakReference<>(factory.inflate(R.layout.dialog_help, null));
        }

        @Override
        protected Void doInBackground(Void... params) {
            mViewHolder.settingsDialog.setTitle(R.string.action_settings);
            mViewHolder.settingsDialog.setIcon(context.get().getResources().getDrawable(R.drawable.ic_tune_24dp));

            mViewHolder.seePacket = viewSettings.get().findViewById(R.id.check_packet);
            mViewHolder.floatMode = viewSettings.get().findViewById(R.id.check_float);
            mViewHolder.pingOnly = viewSettings.get().findViewById(R.id.check_pingolny);
            mViewHolder.spinnerInterval = viewSettings.get().findViewById(R.id.spinner_interval);
            mViewHolder.inputCustomCount = viewSettings.get().findViewById(R.id.input_custom_count);
            mViewHolder.layoutCustomCount = viewSettings.get().findViewById(R.id.layout3);
            mViewHolder.spinnerCount = viewSettings.get().findViewById(R.id.spinner_count);
            mViewHolder.spinnerPingMode = viewSettings.get().findViewById(R.id.spiner_mode);
            mViewHolder.spinnerTextSize = viewSettings.get().findViewById(R.id.spiner_text_size);
            mViewHolder.buttonReset = viewSettings.get().findViewById(R.id.button_reset);
            mViewHolder.customInterval = viewSettings.get().findViewById(R.id.layout5);
            mViewHolder.inputCustom = viewSettings.get().findViewById(R.id.input_custom);
            mViewHolder.inputBytes = viewSettings.get().findViewById(R.id.input_bytes);
            mViewHolder.helpEnablePacket = viewSettings.get().findViewById(R.id.button_helppacket);
            mViewHolder.helpTextSize = viewSettings.get().findViewById(R.id.button_text_size);
            mViewHolder.helpCount = viewSettings.get().findViewById(R.id.button_helpcount);
            mViewHolder.helpInterval = viewSettings.get().findViewById(R.id.button_helpinterval);
            mViewHolder.helpPingOnly = viewSettings.get().findViewById(R.id.button_pingonly);
            mViewHolder.helpPingMode = viewSettings.get().findViewById(R.id.button_helpmode);
            mViewHolder.helpBytes = viewSettings.get().findViewById(R.id.button_helpbytes);
            mViewHolder.helpFloatMode = viewSettings.get().findViewById(R.id.button_floatmode);
            mViewHolder.layoutSettingTextSize = viewSettings.get().findViewById(R.id.layout_text_size);

            if((Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH)) {
                mViewHolder.seePacket.setButtonDrawable(context.get().getResources().getDrawable(R.drawable.custom_checkbox));
                mViewHolder.floatMode.setButtonDrawable(context.get().getResources().getDrawable(R.drawable.custom_checkbox));
                mViewHolder.pingOnly.setButtonDrawable(context.get().getResources().getDrawable(R.drawable.custom_checkbox));
            }

            mViewHolder.floatMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    if (isChecked)
                    {
                        if(mViewHolder.layoutSettingTextSize.getVisibility() != View.VISIBLE)
                            mViewHolder.layoutSettingTextSize.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if(mViewHolder.layoutSettingTextSize.getVisibility() != View.GONE)
                            mViewHolder.layoutSettingTextSize.setVisibility(View.GONE);

                    }

                }
            });
            ArrayAdapter<CharSequence> spinnerCountAdapter = ArrayAdapter.createFromResource(context.get(), R.array.count_array, R.layout.custom_spinner);
            spinnerCountAdapter.setDropDownViewResource(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH ? R.layout.custom_dropdown_item : android.R.layout.simple_spinner_dropdown_item);
            mViewHolder.spinnerCount.setAdapter(spinnerCountAdapter);//center spinner text

            ArrayAdapter<CharSequence> spinnerIntervalAdapter = ArrayAdapter.createFromResource(context.get(), R.array.velocity_array, R.layout.custom_spinner);
            spinnerIntervalAdapter.setDropDownViewResource(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH ? R.layout.custom_dropdown_item : android.R.layout.simple_spinner_dropdown_item);
            mViewHolder.spinnerInterval.setAdapter(spinnerIntervalAdapter);//center spinner text

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (mSecurityPreferences.get().getSetting(PingConstants.PING_MODE).equals(PingConstants.IPV4_IPV6))
                    mSecurityPreferences.get().saveSetting(PingConstants.PING_MODE, PingConstants.IPV4_ONLY);

                ArrayAdapter<String> spinnerPingAdapter = new ArrayAdapter<String>(activity.get(), R.layout.custom_spinner, Arrays.asList(activity.get().getResources().getStringArray(R.array.array_mode))) {
                    @Override
                    public boolean isEnabled(int position) {
                        // TODO Auto-generated method stub
                        return position != 1;
                    }

                    // Change color item
                    @Override
                    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                        // TODO Auto-generated method stub
                        View mView = super.getDropDownView(position, convertView, parent);
                        TextView mTextView = (TextView) mView;
                        if (position == 1) {
                            mTextView.setTextColor(Color.GRAY);
                        } else {
                            mTextView.setTextColor(Color.BLACK);
                        }
                        return mView;
                    }
                };
                spinnerPingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mViewHolder.spinnerPingMode.setAdapter(spinnerPingAdapter);//center spinner text

            }
            else
            {
                ArrayAdapter<CharSequence> spinnerPingAdapter = ArrayAdapter.createFromResource(context.get(), R.array.array_mode, R.layout.custom_spinner);
                spinnerPingAdapter.setDropDownViewResource(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH ? R.layout.custom_dropdown_item : android.R.layout.simple_spinner_dropdown_item);
                mViewHolder.spinnerPingMode.setAdapter(spinnerPingAdapter);//center spinner text
            }

            ArrayAdapter<CharSequence> spinnerTextSize = ArrayAdapter.createFromResource(context.get(), R.array.array_text_size, R.layout.custom_spinner);
            spinnerTextSize.setDropDownViewResource(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH ? R.layout.custom_dropdown_item : android.R.layout.simple_spinner_dropdown_item);
            mViewHolder.spinnerTextSize.setAdapter(spinnerTextSize);//center spinner text

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                mViewHolder.spinnerPingMode.setPopupBackgroundResource(R.drawable.spinner_dropdown_background);
                mViewHolder.spinnerCount.setPopupBackgroundResource(R.drawable.spinner_dropdown_background);
                mViewHolder.spinnerTextSize.setPopupBackgroundResource(R.drawable.spinner_dropdown_background);
                mViewHolder.spinnerInterval.setPopupBackgroundResource(R.drawable.spinner_dropdown_background);
            }

            mViewHolder.helpPingOnly.setOnClickListener(listener.get());
            mViewHolder.helpCount.setOnClickListener(listener.get());
            mViewHolder.helpEnablePacket.setOnClickListener(listener.get());
            mViewHolder.helpTextSize.setOnClickListener(listener.get());
            mViewHolder.helpInterval.setOnClickListener(listener.get());
            mViewHolder.helpPingMode.setOnClickListener(listener.get());
            mViewHolder.helpBytes.setOnClickListener(listener.get());
            mViewHolder.helpFloatMode.setOnClickListener(listener.get());
            mViewHolder.buttonReset.setOnClickListener(listener.get());

            mViewHolder.inputBytes.setText(mSecurityPreferences.get().getSetting(PingConstants.PACKET_BYTES));

            mViewHolder.settingsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (mViewHolder.customInterval.getVisibility() == View.VISIBLE && mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL) == PingConstants.CUSTOM && mViewHolder.inputCustom.getText().toString().equals(""))
                        mViewHolder.inputCustom.setText(mSecurityPreferences.get().getSetting(PingConstants.INTERVAL));

                    if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE && mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_COUNT) == PingConstants.COUNT_CUSTOM && mViewHolder.inputCustomCount.getText().toString().equals(""))
                        mViewHolder.inputCustomCount.setText(mSecurityPreferences.get().getSetting(PingConstants.COUNT_PING));

                    if (mViewHolder.inputCustom.getError() != null)
                        mViewHolder.inputCustom.setError(null);

                    if (mViewHolder.inputCustomCount.getError() != null)
                        mViewHolder.inputCustomCount.setError(null);

                    if (mViewHolder.inputBytes.getError() != null)
                        mViewHolder.inputBytes.setError(null);

                    mViewHolder.inputBytes.setText(mSecurityPreferences.get().getSetting(PingConstants.PACKET_BYTES));

                    if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_PACKETLOSS).equals(PingConstants.YES))
                        mViewHolder.seePacket.setChecked(true);
                    else
                        mViewHolder.seePacket.setChecked(false);

                    if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_FLOATMODE).equals(PingConstants.YES))
                        mViewHolder.floatMode.setChecked(true);
                    else
                        mViewHolder.floatMode.setChecked(false);

                    if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_PINGONLY).equals(PingConstants.YES))
                        mViewHolder.pingOnly.setChecked(true);
                    else
                        mViewHolder.pingOnly.setChecked(false);

                    mViewHolder.spinnerInterval.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL));
                    mViewHolder.spinnerCount.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_COUNT));
                    mViewHolder.spinnerPingMode.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_MODE));
                    mViewHolder.spinnerTextSize.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_TEXT_SIZE));

                }
            });

            mViewHolder.spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (mViewHolder.spinnerInterval.getSelectedItemPosition() == PingConstants.CUSTOM) {
                        mViewHolder.customInterval.setVisibility(View.VISIBLE);
                        if (mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL) == PingConstants.CUSTOM) {
                            mViewHolder.inputCustom.setText(mSecurityPreferences.get().getSetting(PingConstants.INTERVAL));
                        }
                    } else {
                        if (mViewHolder.customInterval.getVisibility() != View.GONE) {
                            mViewHolder.customInterval.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    //--
                }
            });

            mViewHolder.spinnerCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (mViewHolder.spinnerCount.getSelectedItemPosition() == PingConstants.COUNT_CUSTOM) {
                        mViewHolder.layoutCustomCount.setVisibility(View.VISIBLE);
                        if (mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_COUNT) == PingConstants.COUNT_CUSTOM) {
                            mViewHolder.inputCustomCount.setText(mSecurityPreferences.get().getSetting(PingConstants.COUNT_PING));
                        }
                    } else {
                        if (mViewHolder.layoutCustomCount.getVisibility() != View.GONE) {
                            mViewHolder.layoutCustomCount.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    //--
                }
            });

            mViewHolder.spinnerPingMode.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_MODE));

            mViewHolder.spinnerTextSize.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_TEXT_SIZE));

            if (mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL) == PingConstants.CUSTOM) {
                mViewHolder.spinnerInterval.setSelection(PingConstants.CUSTOM);
                mViewHolder.customInterval.setVisibility(View.VISIBLE);
                mViewHolder.inputCustom.setText(mSecurityPreferences.get().getSetting(PingConstants.INTERVAL));
            } else
                mViewHolder.spinnerInterval.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL));

            if (mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_COUNT) == PingConstants.COUNT_CUSTOM) {
                mViewHolder.spinnerCount.setSelection(PingConstants.COUNT_CUSTOM);
                mViewHolder.layoutCustomCount.setVisibility(View.VISIBLE);
                mViewHolder.inputCustomCount.setText(mSecurityPreferences.get().getSetting(PingConstants.COUNT_PING));
            } else
                mViewHolder.spinnerCount.setSelection(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_COUNT));

            if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_PACKETLOSS).equals(PingConstants.YES)) {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.seePacket.setChecked(true);
                    }
                });
            } else {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.seePacket.setChecked(false);
                    }
                });
            }
            if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_FLOATMODE).equals(PingConstants.YES)) {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.floatMode.setChecked(true);
                    }
                });
            } else {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.floatMode.setChecked(false);
                    }
                });
            }
            if (mSecurityPreferences.get().getSetting(PingConstants.ENABLE_PINGONLY).equals(PingConstants.YES)) {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.pingOnly.setChecked(true);
                    }
                });
            } else {
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mViewHolder.pingOnly.setChecked(false);
                    }
                });
            }
            mViewHolder.settingsDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE)
                        mViewHolder.inputCustomCount.setSelection(mViewHolder.inputCustomCount.getText().length());

                    if (mViewHolder.customInterval.getVisibility() == View.VISIBLE)
                        mViewHolder.inputCustom.setSelection(mViewHolder.inputCustom.getText().length());

                    mViewHolder.inputBytes.setSelection(mViewHolder.inputBytes.getText().length());

                    Button button = mViewHolder.settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {//Ao clicar no botão de salvar
                            if (mViewHolder.customInterval.getVisibility() == View.VISIBLE && mViewHolder.inputCustom.getText().toString().equals("")) {
                                mViewHolder.inputCustom.setError(context.get().getString(R.string.required_text));
                            }
                            else if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE && mViewHolder.inputCustomCount.getText().toString().equals("")) {
                                mViewHolder.inputCustomCount.setError(context.get().getString(R.string.required_text));
                            }
                            else if (mViewHolder.customInterval.getVisibility() == View.VISIBLE && mViewHolder.inputCustom.getText().toString().length() > 6)//Maior que 999999
                            {
                                mViewHolder.inputCustom.setError(context.get().getString(R.string.required_interval));
                            }
                            else if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE && mViewHolder.inputCustomCount.getText().toString().length() > 11)//Maior que 99999999999
                            {
                                mViewHolder.inputCustomCount.setError(context.get().getString(R.string.required_count));
                            }
                            else if (mViewHolder.layoutCustomCount.getVisibility() == View.VISIBLE && mViewHolder.inputCustomCount.getText().toString().substring(0, 1).equals("0"))//começa com 0 ou for igual a 0
                            {
                                mViewHolder.inputCustomCount.setError(context.get().getString(R.string.required_count));
                            }
                            else if (mViewHolder.customInterval.getVisibility() == View.VISIBLE && Double.parseDouble(mViewHolder.inputCustom.getText().toString()) <= 0.1) {
                                mViewHolder.inputCustom.setError(context.get().getString(R.string.required_interval));
                            }
                            else if (Integer.parseInt(mViewHolder.inputBytes.getText().toString()) > 65507) {
                                mViewHolder.inputBytes.setError(context.get().getString(R.string.required_bytes));
                            }
                            else if (mViewHolder.customInterval.getVisibility() == View.VISIBLE && mViewHolder.inputCustom.getText().toString().contains("-")) {
                                mViewHolder.inputCustom.setError(context.get().getString(R.string.prevent_trace));
                            } else {
                                if (mViewHolder.seePacket.isChecked()) {
                                    mViewHolder.packetLayout.setVisibility(View.VISIBLE);
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_PACKETLOSS, PingConstants.YES);
                                } else {
                                    mViewHolder.packetLayout.setVisibility(View.INVISIBLE);
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_PACKETLOSS, PingConstants.NO);
                                }

                                if (mViewHolder.pingOnly.isChecked())
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_PINGONLY, PingConstants.YES);
                                else
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_PINGONLY, PingConstants.NO);

                                if (mViewHolder.floatMode.isChecked()) {
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_FLOATMODE, PingConstants.YES);
                                    mViewHolder.buttonPing.setText(activity.get().getString(R.string.button_ping_float));
                                }
                                else {
                                    mSecurityPreferences.get().saveSetting(PingConstants.ENABLE_FLOATMODE, PingConstants.NO);
                                    mViewHolder.buttonPing.setText(activity.get().getString(R.string.ping));
                                }

                                mSecurityPreferences.get().saveSetting(PingConstants.INTERVAL, mViewHolder.spinnerInterval.getSelectedItemPosition() == PingConstants.CUSTOM ? mViewHolder.inputCustom.getText().toString() : mSecurityPreferences.get().convertSpinner(mViewHolder.spinnerInterval.getSelectedItemPosition(), PingConstants.SPINNER_INTERVAL));
                                mSecurityPreferences.get().saveSetting(PingConstants.COUNT_PING, mViewHolder.spinnerCount.getSelectedItemPosition() == PingConstants.COUNT_CUSTOM ? mViewHolder.inputCustomCount.getText().toString() : mSecurityPreferences.get().convertSpinner(mViewHolder.spinnerCount.getSelectedItemPosition(), PingConstants.SPINNER_COUNT));
                                mSecurityPreferences.get().saveSetting(PingConstants.PING_MODE, mSecurityPreferences.get().convertSpinner(mViewHolder.spinnerPingMode.getSelectedItemPosition(), PingConstants.SPINNER_MODE));
                                mSecurityPreferences.get().saveSetting(PingConstants.TEXT_SIZE, mSecurityPreferences.get().convertSpinner(mViewHolder.spinnerTextSize.getSelectedItemPosition(), PingConstants.SPINNER_TEXT_SIZE));
                                mSecurityPreferences.get().saveSetting(PingConstants.PACKET_BYTES, mViewHolder.inputBytes.getText().toString());

                                mViewHolder.intervalSpeed.setText(mSecurityPreferences.get().getSpinnerText(mSecurityPreferences.get().getSpinner(PingConstants.SPINNER_INTERVAL), context.get()));
                                Toast.makeText(context.get(), context.get().getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
                                mViewHolder.settingsDialog.dismiss();
                            }
                        }
                    });
                }
            });
            mViewHolder.settingsDialog.setView(viewSettings.get());
            mViewHolder.settingsDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.get().getString(R.string.button_save),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
            mViewHolder.settingsDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.get().getString(R.string.close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
            //--------------------------------------------------Help Dialog-------------------------------------------------------
            mViewHolder.helpDialog.setTitle(context.get().getString(R.string.ajuda));
            Drawable icon = ContextCompat.getDrawable(context.get(), R.drawable.ic_help_outline_24dp).mutate();
            icon.setColorFilter(new ColorMatrixColorFilter(new float[]{
                    -1, 0, 0, 0, 0, // red
                    0, -1, 0, 0, 0, // green
                    0, 0, -1, 0, 0, // blue
                    0, 0, 0, 1, 0     // alpha
            }));
            mViewHolder.helpDialog.setIcon(icon);

            mViewHolder.helpDialog.setView(viewHelp.get());
            mViewHolder.helpDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.get().getString(R.string.close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
            //------------------------------------------------HELP ITEM DIALOG---------------------------------------------------
            mViewHolder.helpItem.setTitle(context.get().getString(R.string.tab_info));
            icon = ContextCompat.getDrawable(context.get(), R.drawable.ic_info_outline_24dp).mutate();
            icon.setColorFilter(new ColorMatrixColorFilter(new float[]{
                    -1, 0, 0, 0, 0, // red
                    0, -1, 0, 0, 0, // green
                    0, 0, -1, 0, 0, // blue
                    0, 0, 0, 1, 0     // alpha
            }));
            mViewHolder.helpItem.setIcon(icon);
            mViewHolder.helpItem.setButton(AlertDialog.BUTTON_NEGATIVE, context.get().getString(R.string.close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
        }
    }
}