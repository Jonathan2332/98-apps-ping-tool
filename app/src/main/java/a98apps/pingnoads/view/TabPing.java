package a98apps.pingnoads.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import a98apps.pingnoads.constants.PingConstants;
import a98apps.pingnoads.floating.window.PingFloatWindow;
import a98apps.pingnoads.util.Ping;
import a98apps.pingnoads.R;
import a98apps.pingnoads.util.SecurityPreferences;
import wei.mark.standout.StandOutWindow;

public class TabPing extends Fragment implements View.OnClickListener
{
    private Ping preparePing;
    private ArrayAdapter<Spanned> adapter;
    private List<String> addressRecent = new ArrayList<>();
    private ViewHolder mViewHolder;
    private SecurityPreferences mSecurityPreferences;
    private final Handler spinnerHandler = new Handler();
    private Runnable spinnerRunnable;
    private boolean flag = false;

    TabPing newInstance(ViewHolder m, SecurityPreferences s)
    {
        this.mViewHolder = m;
        this.mSecurityPreferences = s;
        return this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_pageping, container, false);

        mViewHolder.packetLayout = rootView.findViewById(R.id.packet_layout);
        mViewHolder.buttonPing = rootView.findViewById(R.id.button_ping);
        mViewHolder.inputHost = rootView.findViewById(R.id.input_host);
        mViewHolder.totalPacketLoss = rootView.findViewById(R.id.result_packet);
        mViewHolder.listaPing = rootView.findViewById(R.id.lista);
        mViewHolder.intervalSpeed = rootView.findViewById(R.id.text_interval);
        mViewHolder.spinnerRecent = rootView.findViewById(R.id.spinner_recent);

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
            mViewHolder.inputHost.setCursorVisible(false);//Android pie já inicia com o cursor desabilitado
        }

        mViewHolder.inputHost.setOnClickListener(TabPing.this);

        mViewHolder.buttonPing.setOnClickListener(TabPing.this);

        mViewHolder.intervalSpeed.setText(mSecurityPreferences.getSpinnerText(mSecurityPreferences.getSpinner(PingConstants.SPINNER_INTERVAL), getContext()));

        adapter = new ArrayAdapter<>(getActivity(), R.layout.custom_listview, new ArrayList<Spanned>());
        mViewHolder.listaPing.setAdapter(adapter);

        ArrayAdapter<String> spinnerRecentAdapter = new ArrayAdapter<String>(getActivity(), R.layout.custom_spinner, addressRecent = mSecurityPreferences.getArray()){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view;
                TextView tv = new TextView(getContext());
                tv.setHeight(0);
                tv.setVisibility(View.GONE);
                view = tv;
                return view;
            }
        };
        spinnerRecentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mViewHolder.spinnerRecent.setAdapter(spinnerRecentAdapter);

        mViewHolder.spinnerRecent.post(new Runnable() {
            @Override public void run() {
                mViewHolder.spinnerRecent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        mViewHolder.inputHost.setText(mViewHolder.spinnerRecent.getSelectedItem().toString());
                        mViewHolder.inputHost.setSelection(mViewHolder.inputHost.getText().length());
                        Collections.swap(addressRecent, position, 0);
                        mViewHolder.spinnerRecent.setSelection(0);
                        mSecurityPreferences.saveArray(addressRecent);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });
        mViewHolder.spinnerRecent.post(new Runnable() {
            @Override public void run() {
                mViewHolder.spinnerRecent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                        if(mViewHolder.spinnerRecent.getSelectedItemPosition() == 0 && !mViewHolder.spinnerRecent.getSelectedItem().equals(mViewHolder.inputHost.getText().toString())) {
                            mViewHolder.inputHost.setText(mViewHolder.spinnerRecent.getSelectedItem().toString());
                            mViewHolder.inputHost.setSelection(mViewHolder.inputHost.getText().length());
                        }
                    }
                });
            }
        });

        if(mViewHolder.spinnerRecent.getItemAtPosition(0) != null)
            mViewHolder.inputHost.setText(mViewHolder.spinnerRecent.getItemAtPosition(0).toString());

        if(!mSecurityPreferences.getSetting(PingConstants.ENABLE_PACKETLOSS).equals(PingConstants.YES))
            mViewHolder.packetLayout.setVisibility(View.INVISIBLE);

        if(mSecurityPreferences.getSetting(PingConstants.ENABLE_FLOATMODE).equals(PingConstants.YES))
            mViewHolder.buttonPing.setText(getString(R.string.button_ping_float));

        mViewHolder.inputHost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                resetStatusSpinner();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(null);
    }
    public void addItems(final String text)
    {
        adapter.add(Html.fromHtml(text));
    }
    public void resetStatusSpinner()
    {
        if(mViewHolder.spinnerRecent.getVisibility() != View.VISIBLE) {
            mViewHolder.spinnerRecent.setVisibility(View.VISIBLE);
            spinnerHandler.removeCallbacks(spinnerRunnable);
        }
    }
    public void resetRecentSpinner()
    {
        addressRecent.clear();
        addressRecent.addAll(mSecurityPreferences.getArray());
    }
    private void clearItems()
    {
        adapter.clear();
    }
    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.button_ping)
        {
            if (preparePing == null)
            {
                String url = mViewHolder.inputHost.getText().toString().replaceAll("\\s+","");
                if (url.equals(""))
                {
                    mViewHolder.spinnerRecent.setVisibility(View.GONE);
                    mViewHolder.inputHost.setError(getString(R.string.host_empty));
                    spinnerHandler.postDelayed(spinnerRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mViewHolder.inputHost.setError(null);
                            mViewHolder.spinnerRecent.setVisibility(View.VISIBLE);
                        }
                    }, 2000);
                    return;
                }
                clearItems();
                if (getActivity().getCurrentFocus() != null)
                {
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                if(!addressRecent.contains(url)) {
                    addressRecent.add(url);
                    Collections.swap(addressRecent, addressRecent.size()-1, 0);
                    mViewHolder.spinnerRecent.setSelection(0);
                    mSecurityPreferences.saveArray(addressRecent);
                }
                else
                {
                    Collections.swap(addressRecent, addressRecent.indexOf(url), 0);
                    mViewHolder.spinnerRecent.setSelection(0);
                    mSecurityPreferences.saveArray(addressRecent);
                }
                if(mSecurityPreferences.getSetting(PingConstants.ENABLE_FLOATMODE).equals(PingConstants.YES))
                {
                    if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(getContext())) {

                        final AlertDialog permissionDialog = new AlertDialog.Builder(getContext()).create();
                        permissionDialog.setTitle(getString(R.string.permission_required_text));
                        permissionDialog.setMessage(getString(R.string.give_permission_text));
                        permissionDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getActivity().getPackageName()));
                                        startActivityForResult(intent, 1);
                                    }
                                });
                        permissionDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                permissionDialog.dismiss();
                            }
                        });
                        permissionDialog.show();
                    }
                    else
                    {
                        if(mSecurityPreferences.getSetting(PingConstants.ENABLE_PINGONLY).equals(PingConstants.NO))
                        {
                            addItems("<font color='red'><b>" + getResources().getString(R.string.warning_pingfloat) + "</b></font>");
                        }
                        else
                        {
                            flag = true;
                            StandOutWindow.show(getContext(), PingFloatWindow.class, StandOutWindow.DEFAULT_ID, url);
                            getActivity().finish();
                        }
                    }
                }
                else
                {
                    mViewHolder.inputHost.setCursorVisible(false);
                    preparePing = new Ping(getActivity(),url, mViewHolder,mSecurityPreferences, this);

                    mViewHolder.buttonPing.setText(getString(R.string.parar));
                    mViewHolder.buttonPing.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_corner_button_red));
                    mViewHolder.totalPacketLoss.setText(getResources().getText(R.string.packet_initial));
                    mViewHolder.totalPacketLoss.setTextColor(getResources().getColor(R.color.colorPrimaryText));
                }
            }
            else
            {
                preparePing.interrupt();
                preparePing = null;
                mViewHolder.buttonPing.setText(getString(R.string.ping));
                mViewHolder.buttonPing.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_corner_button));
            }
        }
        else if(id == R.id.input_host)
        {
            mViewHolder.inputHost.setCursorVisible(true);
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(preparePing != null)
        {
            preparePing.interrupt();
            preparePing = null;
        }

        if (getActivity().isFinishing() && !flag)
            Process.killProcess(Process.myPid());
    }
}