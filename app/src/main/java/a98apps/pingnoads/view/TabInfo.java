package a98apps.pingnoads.view;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import a98apps.pingnoads.BuildConfig;
import a98apps.pingnoads.R;
import a98apps.pingnoads.network.NetworkStatus;

public class TabInfo extends Fragment implements View.OnClickListener{
    private NetworkStatus receiver;
    private IntentFilter intentFilter;
    private LoadTabInfo loadTabTask;
    private ViewHolder mViewHolder;

    TabInfo newInstance(ViewHolder holder)
    {
        this.mViewHolder = holder;
        this.receiver = new NetworkStatus(holder);
        this.intentFilter = new IntentFilter();
        return this;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_pageinfo, container, false);
        rootView.setVisibility(View.GONE);
        loadTabTask = new LoadTabInfo(mViewHolder, rootView, getActivity(), receiver, intentFilter, this);
        loadTabTask.execute();
        return rootView;
    }
    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.button_load_ip) {
            mViewHolder.buttonLoadIp.setText(getResources().getString(R.string.getting_mask));
            mViewHolder.buttonLoadIp.setClickable(false);
            mViewHolder.buttonLoadIp.setFocusable(false);
            mViewHolder.buttonLoadIp.setTextColor(getResources().getColor(R.color.colorAccent2));
            receiver.loadExternalIp(getContext());
        }
    }
    private static class LoadTabInfo extends AsyncTask<Void, Void, Void> {
        private final ViewHolder mViewHolder;
        private final WeakReference<View> rootView;
        private final WeakReference<Activity> activity;
        private final WeakReference<NetworkStatus> receiver;
        private final WeakReference<IntentFilter> intentFilter;
        private final WeakReference<View.OnClickListener> listener;
        private LoadTabInfo(ViewHolder mViewHolder, View v, Activity a, NetworkStatus n, IntentFilter i, View.OnClickListener l)
        {
            this.mViewHolder = mViewHolder;
            this.rootView = new WeakReference<>(v);
            this.activity = new WeakReference<>(a);
            this.receiver = new WeakReference<>(n);
            this.intentFilter = new WeakReference<>(i);
            this.listener = new WeakReference<>(l);
        }
        @Override
        protected Void doInBackground(Void... params) {

            mViewHolder.addressIpv4 = rootView.get().findViewById(R.id.text_ipv4);
            mViewHolder.addressIpv6 = rootView.get().findViewById(R.id.text_ipv6);
            mViewHolder.connectionType = rootView.get().findViewById(R.id.connection_type_text);
            mViewHolder.wifiInfo = rootView.get().findViewById(R.id.view_wifi_info);
            mViewHolder.gatewayText = rootView.get().findViewById(R.id.text_gateway);
            mViewHolder.dns1Text = rootView.get().findViewById(R.id.dns1_text);
            mViewHolder.dns2Text = rootView.get().findViewById(R.id.dns2_text);
            mViewHolder.maskText = rootView.get().findViewById(R.id.mask_text);
            mViewHolder.macText = rootView.get().findViewById(R.id.text_mac);
            mViewHolder.macLayout = rootView.get().findViewById(R.id.mac_layout);
            mViewHolder.mobileDns = rootView.get().findViewById(R.id.view_mobile_info_dns);
            mViewHolder.operatorLayout = rootView.get().findViewById(R.id.operator_layout);
            mViewHolder.typeConnectionLayout = rootView.get().findViewById(R.id.connection_type_name_layout);
            mViewHolder.typeConnectionName = rootView.get().findViewById(R.id.connection_type_name_text);
            mViewHolder.operatorText = rootView.get().findViewById(R.id.text_operator);
            mViewHolder.mobileDnsOther = rootView.get().findViewById(R.id.view_mobile_info_dns_other);
            mViewHolder.dns1MobileText = rootView.get().findViewById(R.id.dns1_mobile_text);
            mViewHolder.dns2MobileText = rootView.get().findViewById(R.id.dns2_mobile_text);
            mViewHolder.dns3MobileText = rootView.get().findViewById(R.id.dns3_mobile_text);
            mViewHolder.dns4MobileText = rootView.get().findViewById(R.id.dns4_mobile_text);
            mViewHolder.ipv4Layout = rootView.get().findViewById(R.id.address_ipv4_layout);
            mViewHolder.ipv6Layout = rootView.get().findViewById(R.id.address_ipv6_layout);
            mViewHolder.buttonLoadIp = rootView.get().findViewById(R.id.button_load_ip);
            mViewHolder.layoutExternalIp = rootView.get().findViewById(R.id.external_ip);
            mViewHolder.version = rootView.get().findViewById(R.id.version);

            mViewHolder.buttonLoadIp.setOnClickListener(listener.get());

            activity.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mViewHolder.version.append(activity.get().getString(R.string.version) + BuildConfig.VERSION_NAME);//fix crash on android 4.2
                }
            });
            intentFilter.get().addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            activity.get().registerReceiver(receiver.get(), intentFilter.get());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            rootView.get().setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(null);
    }
    @Override
    public void onResume() {
        super.onResume();
        if(loadTabTask.getStatus() == AsyncTask.Status.FINISHED) {
            getActivity().registerReceiver(receiver, intentFilter);
        }
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if(loadTabTask.getStatus() == AsyncTask.Status.FINISHED) {
            receiver.unregisterListener();
            receiver.stopReceiver();
            getActivity().unregisterReceiver(receiver);
        }
    }
}
