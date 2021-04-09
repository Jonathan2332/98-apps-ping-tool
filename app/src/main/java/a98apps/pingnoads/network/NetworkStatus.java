package a98apps.pingnoads.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import a98apps.pingnoads.R;
import a98apps.pingnoads.view.ViewHolder;

public class NetworkStatus extends BroadcastReceiver
{
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private MobileDataStatus managerListener;
    private final ViewHolder mViewHolder;
    private RunnableIpv6 runIpv6;
    private RunnableDNS runDNS;
    private RunnableMask runMask;
    private TelephonyManager manager;
    private LoadExternalIp externalIpTask;
    private Thread threadExternalIp;
    private int attemptIpv6;
    private int attemptDNS;
    private boolean isRunningIPV6 = false;
    private boolean isRunningDNS = false;
    private boolean isRunningMask = false;

    public NetworkStatus(ViewHolder holder)
    {
        this.mViewHolder = holder;
    }

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        String type = getConnectionType(context, false);
        manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (type)
        {
            case "no_connection":
                unregisterListener();

                if(isRunningIPV6){ mHandler.removeCallbacks(runIpv6); isRunningIPV6 = false; attemptIpv6 = 0; }
                if(isRunningDNS){ mHandler.removeCallbacks(runDNS); isRunningDNS = false; attemptDNS = 0; }
                if(isRunningMask){ mHandler.removeCallbacks(runMask); isRunningMask = false; }

                if(externalIpTask != null)
                    externalIpTask.cancel(true);

                if(threadExternalIp != null && threadExternalIp.isAlive())
                    threadExternalIp.interrupt();

                if(mViewHolder.layoutExternalIp.getVisibility() != View.GONE) {
                    mViewHolder.layoutExternalIp.setVisibility(View.GONE);
                }

                mViewHolder.buttonLoadIp.setText(context.getResources().getString(R.string.text_button_load));
                mViewHolder.buttonLoadIp.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.buttonLoadIp.setClickable(false);
                mViewHolder.buttonLoadIp.setFocusable(false);

                if (mViewHolder.wifiInfo.getVisibility() != View.GONE && mViewHolder.macLayout.getVisibility() != View.GONE) {
                    mViewHolder.wifiInfo.setVisibility(View.GONE);
                    mViewHolder.macLayout.setVisibility(View.GONE);
                }
                if(mViewHolder.operatorLayout.getVisibility() != View.GONE)
                    mViewHolder.operatorLayout.setVisibility(View.GONE);

                if(mViewHolder.mobileDns.getVisibility() != View.GONE)
                    mViewHolder.mobileDns.setVisibility(View.GONE);

                if(mViewHolder.mobileDnsOther.getVisibility() != View.GONE)
                    mViewHolder.mobileDnsOther.setVisibility(View.GONE);

                if(mViewHolder.typeConnectionLayout.getVisibility() != View.GONE)
                    mViewHolder.typeConnectionLayout.setVisibility(View.GONE);

                if(mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);

                if(mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                mViewHolder.connectionType.setText(R.string.no_connection);
                mViewHolder.connectionType.setTextColor(context.getResources().getColor(R.color.colorRed));
                mViewHolder.addressIpv4.setText(context.getString(R.string.offline_ipv4));
                mViewHolder.addressIpv6.setText(context.getString(R.string.offline_ipv6));
                mViewHolder.addressIpv4.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
                mViewHolder.addressIpv6.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
                break;
            case "WIFI":
                unregisterListener();

                if(isRunningIPV6){ mHandler.removeCallbacks(runIpv6); isRunningIPV6 = false; attemptIpv6 = 0; }
                if(isRunningDNS){ mHandler.removeCallbacks(runDNS); isRunningDNS = false; attemptDNS = 0; }
                if(isRunningMask){ mHandler.removeCallbacks(runMask); isRunningMask = false; }

                if(externalIpTask != null)
                    externalIpTask.cancel(true);

                if(threadExternalIp != null && threadExternalIp.isAlive())
                    threadExternalIp.interrupt();

                if(mViewHolder.layoutExternalIp.getVisibility() != View.VISIBLE) {
                    mViewHolder.layoutExternalIp.setVisibility(View.VISIBLE);
                }

                mViewHolder.buttonLoadIp.setText(context.getResources().getString(R.string.text_button_load));
                mViewHolder.buttonLoadIp.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.buttonLoadIp.setClickable(true);
                mViewHolder.buttonLoadIp.setFocusable(true);

                WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                DhcpInfo d = wifi.getDhcpInfo();

                if (d.netmask != 0)
                    mViewHolder.maskText.setText(convertIntToIp(d.netmask));
                else {
                    mViewHolder.maskText.setText(context.getString(R.string.getting_mask));
                    mHandler.post(runMask = new RunnableMask(convertIntToIp(d.ipAddress), context));
                }

                if(mViewHolder.operatorLayout.getVisibility() != View.GONE)
                    mViewHolder.operatorLayout.setVisibility(View.GONE);

                if(mViewHolder.mobileDns.getVisibility() != View.GONE)
                    mViewHolder.mobileDns.setVisibility(View.GONE);

                if(mViewHolder.mobileDnsOther.getVisibility() != View.GONE)
                    mViewHolder.mobileDnsOther.setVisibility(View.GONE);

                if(mViewHolder.typeConnectionLayout.getVisibility() != View.GONE)
                    mViewHolder.typeConnectionLayout.setVisibility(View.GONE);

                if(mViewHolder.wifiInfo.getVisibility() != View.VISIBLE)
                    mViewHolder.wifiInfo.setVisibility(View.VISIBLE);

                if(mViewHolder.macLayout.getVisibility() != View.VISIBLE)
                    mViewHolder.macLayout.setVisibility(View.VISIBLE);

                if(mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);

                if(mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    String[] ipv4 = getIPV4(context);
                    if(ipv4 != null)
                    {
                        mViewHolder.addressIpv4.setText(ipv4[0]);
                        if (mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                            mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if (mViewHolder.ipv4Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv4Layout.setVisibility(View.GONE);

                        mViewHolder.addressIpv4.setText(context.getString(R.string.offline_ipv4));
                    }
                }
                else
                {
                    mViewHolder.addressIpv4.setText(convertIntToIp(wifi.getConnectionInfo().getIpAddress()));
                }

                mViewHolder.addressIpv4.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.addressIpv6.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.connectionType.setText(type);
                mViewHolder.connectionType.setTextColor(context.getResources().getColor(R.color.colorAccent2));

                if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    String[] ipv6 = getIPV6(context);
                    if(ipv6 != null)
                    {
                        if(ipv6.length == 1)
                        {
                            if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                            mViewHolder.addressIpv6.setText(ipv6[0]);
                            mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, null, ipv6.length), 2000);
                        }
                        else
                        {
                            int index = 0;
                            mViewHolder.addressIpv6.setText("");
                            for (String ip : ipv6)
                            {
                                if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                                mViewHolder.addressIpv6.append(ip);
                                if (index != ipv6.length -1)
                                {
                                    mViewHolder.addressIpv6.append("\n");
                                    index++;
                                }
                            }
                        }
                    }
                    else
                    {
                        if (mViewHolder.ipv6Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv6Layout.setVisibility(View.GONE);

                        mViewHolder.addressIpv6.setText(context.getString(R.string.offline_ipv6));
                        mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, null, 0), 2000);
                    }
                }
                else
                {
                    List<String> listIpv6 = getIPV6AddressWIFI(convertIntToIp(wifi.getConnectionInfo().getIpAddress()));
                    if (listIpv6.size() == 1 && listIpv6.get(0).equals(context.getString(R.string.offline_ipv6)))
                    {
                        if (mViewHolder.ipv6Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv6Layout.setVisibility(View.GONE);

                        mViewHolder.addressIpv6.setText(context.getString(R.string.offline_ipv6));
                    }
                    else if(listIpv6.size() == 1)
                    {
                        if(mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                            mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                        mViewHolder.addressIpv6.setText(listIpv6.get(0));
                        mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, convertIntToIp(wifi.getConnectionInfo().getIpAddress()), listIpv6.size()), 2000);
                    }
                    else
                    {
                        Collections.sort(listIpv6);
                        Collections.reverse(listIpv6);
                        mViewHolder.addressIpv6.setText("");
                        int index = 0;
                        for (String ip : listIpv6)
                        {
                            mViewHolder.addressIpv6.append(ip);
                            if (index != listIpv6.size() - 1) {
                                mViewHolder.addressIpv6.append("\n");
                                index++;
                            }
                        }
                    }
                }
                mViewHolder.gatewayText.setText(convertIntToIp(d.gateway));
                mViewHolder.dns1Text.setText(convertIntToIp(d.dns1));
                if(d.dns2 != 0)
                    mViewHolder.dns2Text.setText(convertIntToIp(d.dns2));
                else
                    mViewHolder.dns2Text.setText(context.getString(R.string.no_dns));

                mViewHolder.macText.setText(wifi.getConnectionInfo().getMacAddress().equals("02:00:00:00:00:00") ? getMacAddress(context) : wifi.getConnectionInfo().getMacAddress());
                break;
            case "2G":
            case "3G":
            case "4G":
                if(isRunningIPV6){ mHandler.removeCallbacks(runIpv6); isRunningIPV6 = false; attemptIpv6 = 0; }
                if(isRunningDNS){ mHandler.removeCallbacks(runDNS); isRunningDNS = false; attemptDNS = 0; }
                if(isRunningMask){ mHandler.removeCallbacks(runMask); isRunningMask = false; }

                if(externalIpTask != null)
                    externalIpTask.cancel(true);

                if(threadExternalIp != null && threadExternalIp.isAlive())
                    threadExternalIp.interrupt();

                if(mViewHolder.layoutExternalIp.getVisibility() != View.VISIBLE) {
                    mViewHolder.layoutExternalIp.setVisibility(View.VISIBLE);
                }

                mViewHolder.buttonLoadIp.setText(context.getResources().getString(R.string.text_button_load));
                mViewHolder.buttonLoadIp.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.buttonLoadIp.setClickable(true);
                mViewHolder.buttonLoadIp.setFocusable(true);

                if (mViewHolder.wifiInfo.getVisibility() != View.GONE && mViewHolder.macLayout.getVisibility() != View.GONE) {
                    mViewHolder.wifiInfo.setVisibility(View.GONE);
                    mViewHolder.macLayout.setVisibility(View.GONE);
                }

                if(mViewHolder.operatorLayout.getVisibility() != View.VISIBLE)
                    mViewHolder.operatorLayout.setVisibility(View.VISIBLE);
                if(mViewHolder.typeConnectionLayout.getVisibility() != View.VISIBLE)
                    mViewHolder.typeConnectionLayout.setVisibility(View.VISIBLE);

                managerListener = new MobileDataStatus(context);
                manager.listen(managerListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

                mViewHolder.operatorText.setText(manager.getSimOperatorName());
                mViewHolder.connectionType.setTextColor(context.getResources().getColor(R.color.colorAccent2));

                mViewHolder.addressIpv4.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.addressIpv6.setTextColor(context.getResources().getColor(R.color.colorAccent2));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    String[] ipv4 = getIPV4(context);
                    if(ipv4 != null)
                    {
                        mViewHolder.addressIpv4.setText(ipv4[0]);
                        if (mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                            mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if (mViewHolder.ipv4Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv4Layout.setVisibility(View.GONE);
                        mViewHolder.addressIpv4.setText(context.getString(R.string.offline_ipv4));
                    }
                }
                else
                {
                    String ipv4 = getIPV4AddressMobile();
                    if (ipv4 != null)
                    {
                        mViewHolder.addressIpv4.setText(ipv4);
                        if (mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                            mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if (mViewHolder.ipv4Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv4Layout.setVisibility(View.GONE);
                        mViewHolder.addressIpv4.setText(context.getString(R.string.offline_ipv4));
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    String[] ipv6 = getIPV6(context);
                    if(ipv6 != null)
                    {
                        if(ipv6.length == 1)
                        {
                            if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                            mViewHolder.addressIpv6.setText(ipv6[0]);
                            mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, null, ipv6.length), 2000);
                        }
                        else
                        {
                            mViewHolder.addressIpv6.setText("");
                            int index = 0;
                            for (String ip : ipv6) {

                                if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                                mViewHolder.addressIpv6.append(ip);
                                if (index != ipv6.length -1) {
                                    mViewHolder.addressIpv6.append("\n");
                                    index++;
                                }
                            }
                        }
                    }
                    else
                    {
                        if (mViewHolder.ipv6Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv6Layout.setVisibility(View.GONE);

                        mViewHolder.addressIpv6.setText(context.getString(R.string.offline_ipv6));
                        mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, null, 0), 2000);
                    }
                }
                else
                {
                    List<String> listIp = getIPV6AddressMobile();
                    if (listIp.size() == 1 && listIp.get(0).equals(context.getString(R.string.offline_ipv6)))
                    {
                        if (mViewHolder.ipv6Layout.getVisibility() != View.GONE)
                            mViewHolder.ipv6Layout.setVisibility(View.GONE);

                        mViewHolder.addressIpv6.setText(context.getString(R.string.offline_ipv6));
                    }
                    else if(listIp.size()  == 1)
                    {
                        if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                            mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                        mViewHolder.addressIpv6.setText(listIp.get(0));
                        mHandler.postDelayed(runIpv6 = new RunnableIpv6(context, type, null, listIp.size()), 2000);
                    }
                    else if (listIp.size() >= 2)
                    {
                        Collections.sort(listIp);
                        Collections.reverse(listIp);
                        mViewHolder.addressIpv6.setText("");
                        int index = 0;
                        for (String ip : listIp)
                        {
                            if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                            mViewHolder.addressIpv6.append(ip);
                            if (index != listIp.size() - 1) {
                                mViewHolder.addressIpv6.append("\n");
                                index++;
                            }
                        }
                    }
                }
                mHandler.postDelayed(runDNS = new RunnableDNS(context, getDNS(context)), 1000);
                break;
            default:
                unregisterListener();

                if(isRunningIPV6){ mHandler.removeCallbacks(runIpv6); isRunningIPV6 = false; attemptIpv6 = 0; }
                if(isRunningDNS){ mHandler.removeCallbacks(runDNS); isRunningDNS = false; attemptDNS = 0; }
                if(isRunningMask){ mHandler.removeCallbacks(runMask); isRunningMask = false; }

                if(externalIpTask != null)
                    externalIpTask.cancel(true);

                if(threadExternalIp != null && threadExternalIp.isAlive())
                    threadExternalIp.interrupt();

                if(mViewHolder.layoutExternalIp.getVisibility() != View.GONE) {
                    mViewHolder.layoutExternalIp.setVisibility(View.GONE);
                }

                mViewHolder.buttonLoadIp.setText(context.getResources().getString(R.string.text_button_load));
                mViewHolder.buttonLoadIp.setTextColor(context.getResources().getColor(R.color.colorAccent2));
                mViewHolder.buttonLoadIp.setClickable(false);
                mViewHolder.buttonLoadIp.setFocusable(false);

                if (mViewHolder.wifiInfo.getVisibility() != View.GONE && mViewHolder.macLayout.getVisibility() != View.GONE) {
                    mViewHolder.wifiInfo.setVisibility(View.GONE);
                    mViewHolder.macLayout.setVisibility(View.GONE);
                }
                if(mViewHolder.operatorLayout.getVisibility() != View.GONE)
                    mViewHolder.operatorLayout.setVisibility(View.GONE);

                if(mViewHolder.mobileDns.getVisibility() != View.GONE)
                    mViewHolder.mobileDns.setVisibility(View.GONE);

                if(mViewHolder.mobileDnsOther.getVisibility() != View.GONE)
                    mViewHolder.mobileDnsOther.setVisibility(View.GONE);

                if(mViewHolder.typeConnectionLayout.getVisibility() != View.GONE)
                    mViewHolder.typeConnectionLayout.setVisibility(View.GONE);

                if(mViewHolder.ipv4Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv4Layout.setVisibility(View.VISIBLE);

                if(mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                mViewHolder.connectionType.setText(type);
                mViewHolder.connectionType.setTextColor(context.getResources().getColor(R.color.colorRed));
                mViewHolder.addressIpv4.setText(context.getString(R.string.mask_unknown));
                mViewHolder.addressIpv6.setText(context.getString(R.string.mask_unknown));
                mViewHolder.addressIpv4.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
                mViewHolder.addressIpv6.setTextColor(context.getResources().getColor(R.color.colorPrimaryText));
                break;
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String[] getIPV4(Context context)
    {
        try
        {
            ArrayList<String> priorityIpArrayList  = new ArrayList<>();
            ArrayList<String> ipArrayList          = new ArrayList<>();
            String index;
            String current;
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    current = connectivityManager.getActiveNetworkInfo().getExtraInfo();
                    index = networkInfo.getExtraInfo();
                    if (networkInfo.isConnected()) {
                        if(current!= null)
                        {
                            if(index != null && index.equals(current)) {
                                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                                List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();

                                if (linkPropertiesHasDefaultRoute(linkProperties)) {

                                    for (LinkAddress element : linkAddresses) {
                                        if (element.getAddress() instanceof Inet4Address) {
                                            String ip = element.getAddress().getHostAddress();
                                            priorityIpArrayList.add(ip.toUpperCase());
                                        }

                                    }

                                } else {

                                    for (LinkAddress element : linkAddresses) {
                                        if (element.getAddress() instanceof Inet4Address) {
                                            String ip = element.getAddress().getHostAddress();
                                            ipArrayList.add(ip);
                                        }

                                    }
                                }
                            }
                        }
                        else if(index == null)//WIFI
                        {
                            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                            List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();

                            if (linkPropertiesHasDefaultRoute(linkProperties)) {

                                for (LinkAddress element : linkAddresses) {
                                    if (element.getAddress() instanceof Inet4Address) {
                                        String ip = element.getAddress().getHostAddress();
                                        priorityIpArrayList.add(ip.toUpperCase());
                                    }

                                }

                            }
                            else
                            {

                                for (LinkAddress element : linkAddresses) {
                                    if (element.getAddress() instanceof Inet4Address) {
                                        String ip = element.getAddress().getHostAddress();
                                        ipArrayList.add(ip);
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (priorityIpArrayList.isEmpty())
            {
                priorityIpArrayList.addAll(ipArrayList);
            }
            if (priorityIpArrayList.size() > 0) {

                return priorityIpArrayList.toArray(new String[0]);

            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String[] getIPV6(Context context)
    {
        try
        {
            ArrayList<String> priorityIpArrayList  = new ArrayList<>();
            ArrayList<String> ipArrayList          = new ArrayList<>();
            String index;
            String current;
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    current = connectivityManager.getActiveNetworkInfo().getSubtypeName();
                    index = networkInfo.getSubtypeName();
                    if (networkInfo.isConnected()) {
                        if(current!= null)
                        {
                            if(index != null && index.equals(current)) {
                                LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                                List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
                                if (linkPropertiesHasDefaultRoute(linkProperties)) {

                                    for (LinkAddress element : linkAddresses) {
                                        if (element.getAddress() instanceof Inet6Address) {
                                            String ip = element.getAddress().getHostAddress();
                                            priorityIpArrayList.add(ip.toUpperCase());
                                        }
                                    }
                                }
                                else
                                {
                                    for (LinkAddress element : linkAddresses)
                                    {
                                        if (element.getAddress() instanceof Inet6Address)
                                        {
                                            String ip = element.getAddress().getHostAddress();
                                            ipArrayList.add(ip);
                                        }

                                    }
                                }
                            }
                        }
                        else if(index == null)//WIFI
                        {
                            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                            List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();

                            if (linkPropertiesHasDefaultRoute(linkProperties)) {

                                for (LinkAddress element : linkAddresses) {
                                    if (element.getAddress() instanceof Inet6Address) {
                                        String ip = element.getAddress().getHostAddress();
                                        priorityIpArrayList.add(ip.toUpperCase());
                                    }

                                }

                            } else {

                                for (LinkAddress element : linkAddresses) {
                                    if (element.getAddress() instanceof Inet6Address) {
                                        String ip = element.getAddress().getHostAddress();
                                        ipArrayList.add(ip);
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (priorityIpArrayList.isEmpty())
            {
                priorityIpArrayList.addAll(ipArrayList);
            }
            if (priorityIpArrayList.size() > 0) {

                return priorityIpArrayList.toArray(new String[0]);

            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean linkPropertiesHasDefaultRoute(LinkProperties linkProperties) {

        for (RouteInfo route : linkProperties.getRoutes()) {
            if (route.isDefaultRoute()) {
                return true;
            }
        }
        return false;
    }
    public void unregisterListener()
    {
        if(managerListener != null)
            manager.listen(managerListener, PhoneStateListener.LISTEN_NONE);
    }
    public void stopReceiver()
    {
        if(isRunningIPV6){ mHandler.removeCallbacks(runIpv6); isRunningIPV6 = false; attemptIpv6 = 0; }
        if(isRunningDNS){ mHandler.removeCallbacks(runDNS); isRunningDNS = false; attemptDNS = 0; }
        if(isRunningMask){ mHandler.removeCallbacks(runMask); isRunningMask = false; }
    }
    private String getMacAddress(Context context) {
        try
        {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return context.getString(R.string.mask_unknown);
    }

    private class RunnableMask implements Runnable {
        private final Context c;
        private final String ip;
        private RunnableMask(String type,Context c)
        {
            this.c = c;
            this.ip = type;
            isRunningMask = true;
        }
        @Override
        public void run()
        {
            try
            {
                InetAddress address = Inet4Address.getByName(ip);
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
                if(networkInterface != null)
                {
                    int mask = networkInterface.getInterfaceAddresses().get(networkInterface.getInterfaceAddresses().size()-1).getNetworkPrefixLength();
                    SubnetUtils utils;
                    if (mask <= 31)
                    {
                        utils = new SubnetUtils(ip + "/" + mask);
                        mViewHolder.maskText.setText(utils.getInfo().getNetmask());
                    }
                    else if (mask == 32) {
                        utils = new SubnetUtils(ip + "/" + mask);
                        utils.setInclusiveHostCount();
                        mViewHolder.maskText.setText(utils.getInfo().getNetmask());
                    }
                    else
                    {
                        mViewHolder.maskText.setText(c.getString(R.string.mask_unknown));
                    }
                    isRunningMask = false;
                    mHandler.removeCallbacks(this);
                }
                else
                {
                    mHandler.postDelayed(this, 3000);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    private String convertIntToIp(int i) {

        return (i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ((i >> 24 ) & 0xFF) ;
    }

    private String getIPV4AddressMobile()
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); )
            {
                NetworkInterface interfaces = en.nextElement();

                for (Enumeration<InetAddress> enumIpAddr = interfaces.getInetAddresses(); enumIpAddr.hasMoreElements(); )
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address && checkInterface(interfaces.getName()))
                    {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    private List<String> getIPV6AddressWIFI(String ip)
    {
        List<String> listAddress = new ArrayList<>();
        try
        {
            NetworkInterface wifiInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ip));
            for (Enumeration<InetAddress> enumIpAddr = wifiInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); )
            {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address)
                {
                    String address = inetAddress.getHostAddress();
                    if (address.contains("%"))
                    {
                        int separator = address.indexOf('%');
                        listAddress.add(separator < 0 ? address.toUpperCase() : address.substring(0, separator).toUpperCase());
                    }
                    else
                    {
                        listAddress.add(address.toUpperCase());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if(listAddress.isEmpty()) listAddress.add("0000:0000:0000:0000:0000");
        return listAddress;
    }
    private List<String> getIPV6AddressMobile()
    {
        List<String> listAddress = new ArrayList<>();
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface interfaces = en.nextElement();

                for (Enumeration<InetAddress> enumIpAddr = interfaces.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet6Address) {
                        String address = inetAddress.getHostAddress();
                        if (checkInterface(interfaces.getName())) {
                            if (address.contains("%")) {
                                int separator = address.indexOf('%');
                                listAddress.add(separator < 0 ? address.toUpperCase() : address.substring(0, separator).toUpperCase());

                            } else {
                                listAddress.add(address.toUpperCase());
                            }
                        }
                    }
                }
                if(!listAddress.isEmpty()) return listAddress;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if(listAddress.isEmpty()) listAddress.add("0000:0000:0000:0000:0000");
        return listAddress;
    }
    private boolean checkInterface(String name)
    {
        return name.contains("0") && (name.contains("eth") || name.contains("radio") || name.contains("rmnet"));
    }
    public static String getConnectionType(Context context, boolean getString)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if(info==null || !info.isConnected()) {
            return getString ? context.getString(R.string.no_connection) : "no_connection";
        }
        else if(info.getType() == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        }
        else if(info.getType() == ConnectivityManager.TYPE_MOBILE)
        {
            int networkType = info.getSubtype();
            switch (networkType)
            {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                case 19:  //LTE_CA
                    return "4G";
                default:
                    return context.getString(R.string.mask_unknown);
            }
        }
        else
        {
            return context.getString(R.string.mask_unknown);
        }
    }
    private int getDNS(Context context)
    {
        DnsServersDetector servers = new DnsServersDetector(context);
        String[] dns = servers.getServers();
        if(dns.length == 1)
        {
            if(mViewHolder.mobileDns.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDns.setVisibility(View.VISIBLE);

            if(mViewHolder.mobileDnsOther.getVisibility() != View.GONE)
                mViewHolder.mobileDnsOther.setVisibility(View.GONE);

            mViewHolder.dns1MobileText.setText(dns[0].toUpperCase());
            mViewHolder.dns2MobileText.setText(context.getString(R.string.no_dns));
        }
        else if(dns.length == 2)
        {
            if(mViewHolder.mobileDns.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDns.setVisibility(View.VISIBLE);

            if(mViewHolder.mobileDnsOther.getVisibility() != View.GONE)
                mViewHolder.mobileDnsOther.setVisibility(View.GONE);

            mViewHolder.dns1MobileText.setText(dns[0].toUpperCase());
            mViewHolder.dns2MobileText.setText(dns[1].toUpperCase());
        }
        else if(dns.length == 3)
        {
            if(mViewHolder.mobileDns.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDns.setVisibility(View.VISIBLE);

            if(mViewHolder.mobileDnsOther.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDnsOther.setVisibility(View.VISIBLE);

            mViewHolder.dns1MobileText.setText(dns[0].toUpperCase());
            mViewHolder.dns2MobileText.setText(dns[1].toUpperCase());
            mViewHolder.dns3MobileText.setText(dns[2].toUpperCase());
            mViewHolder.dns4MobileText.setText(context.getString(R.string.no_dns));
        }
        else if(dns.length == 4)
        {
            if(mViewHolder.mobileDns.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDns.setVisibility(View.VISIBLE);

            if(mViewHolder.mobileDnsOther.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDnsOther.setVisibility(View.VISIBLE);

            mViewHolder.dns1MobileText.setText(dns[0]);
            mViewHolder.dns2MobileText.setText(dns[1]);
            mViewHolder.dns3MobileText.setText(dns[2].toUpperCase());//Ipv6
            mViewHolder.dns4MobileText.setText(dns[3].toUpperCase());//Ipv6
        }
        else if(dns.length >= 5)//prevent crash
        {
            if(mViewHolder.mobileDns.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDns.setVisibility(View.VISIBLE);

            if(mViewHolder.mobileDnsOther.getVisibility() != View.VISIBLE)
                mViewHolder.mobileDnsOther.setVisibility(View.VISIBLE);


            mViewHolder.dns1MobileText.setText(dns[0].toUpperCase());
            mViewHolder.dns2MobileText.setText(dns[1].toUpperCase());
            mViewHolder.dns3MobileText.setText(dns[2].toUpperCase());
            mViewHolder.dns4MobileText.setText(dns[3].toUpperCase());
        }
        return dns.length;
    }
    private class RunnableDNS implements Runnable
    {
        private final Context c;
        private final int size;
        private RunnableDNS(Context c, int s)
        {
            this.c = c;
            this.size = s;
            isRunningDNS = true;
        }

        @Override
        public void run()
        {
            String ipv6Address = mViewHolder.addressIpv6.getText().toString();
            if(size == 2 && !ipv6Address.equals(c.getString(R.string.offline_ipv6)) && !mViewHolder.addressIpv4.getText().toString().equals(c.getString(R.string.offline_ipv4))) {
                if (getDNS(c) == size)
                {
                    if(attemptDNS <= 18)
                    {
                        attemptDNS++;
                        mHandler.postDelayed(this, 3000);
                    }
                    else
                    {
                        attemptDNS = 0;
                        isRunningDNS = false;
                        mHandler.removeCallbacks(this);
                    }
                }
                else
                {
                    attemptDNS = 0;
                    isRunningDNS = false;
                    mHandler.removeCallbacks(this);
                }
            }
            else {
                attemptDNS = 0;
                isRunningDNS = false;
                mHandler.removeCallbacks(this);
            }
        }
    }
    private class RunnableIpv6 implements Runnable {
        private final Context c;
        private final String type;
        private final String ip;
        private int size;

        private RunnableIpv6(Context c, String type, String ip, int size) {
            this.c = c;
            this.type = type;
            this.ip = ip;
            this.size = size;
            isRunningIPV6 = true;
        }

        @Override
        public void run()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                if(!getConnectionType(c, false).equals("no_connection"))
                {
                    String[] ipv6 = getIPV6(c);
                    if(ipv6 != null)
                    {
                        if (size == ipv6.length)
                        {
                            if (attemptIpv6 <= 8)
                            {
                                attemptIpv6++;
                                mHandler.postDelayed(this, 3000);
                            }
                            else
                            {
                                attemptIpv6 = 0;
                                isRunningIPV6 = false;
                                mHandler.removeCallbacks(this);
                            }
                        }
                        else
                        {
                            this.size = ipv6.length;
                            mViewHolder.addressIpv6.setText("");
                            int index = 0;
                            for (String ip : ipv6)
                            {
                                if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                    mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                                mViewHolder.addressIpv6.append(ip);
                                if (index != ipv6.length - 1) {
                                    mViewHolder.addressIpv6.append("\n");
                                    index++;
                                }

                            }
                            if (!type.equals("WIFI")) getDNS(c);
                            mViewHolder.addressIpv6.setTextColor(c.getResources().getColor(R.color.colorAccent2));
                            attemptIpv6++;
                            mHandler.postDelayed(this, 3000);
                        }
                    }
                    else
                    {
                        if (attemptIpv6 <= 13)
                        {
                            attemptIpv6++;
                            mHandler.postDelayed(this, 3000);
                        }
                        else
                        {
                            attemptIpv6 = 0;
                            isRunningIPV6 = false;
                            mHandler.removeCallbacks(this);
                        }
                    }
                }
            }
            else
            {
                if (ip == null)
                {
                    List<String> listIpv6 = getIPV6AddressMobile();
                    if (size == listIpv6.size())
                    {
                        if (attemptIpv6 <= 18)
                        {
                            attemptIpv6++;
                            mHandler.postDelayed(this, 3000);
                        }
                        else
                        {
                            attemptIpv6 = 0;
                            isRunningIPV6 = false;
                            mHandler.removeCallbacks(this);
                        }
                    }
                    else
                    {
                        if (listIpv6.size() >= 2) {
                            Collections.sort(listIpv6);
                            Collections.reverse(listIpv6);
                        }
                        int index = 0;
                        mViewHolder.addressIpv6.setText("");
                        for (String ip : listIpv6)
                        {
                            if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                            mViewHolder.addressIpv6.append(ip);
                            if (index != listIpv6.size() - 1) {
                                mViewHolder.addressIpv6.append("\n");
                                index++;
                            }
                        }
                        if (!type.equals("WIFI")) getDNS(c);
                        mViewHolder.addressIpv6.setTextColor(c.getResources().getColor(R.color.colorAccent2));
                        attemptIpv6 = 0;
                        isRunningIPV6 = false;
                        mHandler.removeCallbacks(this);
                    }
                }
                else//WIFI------------------------------------------------------------
                {
                    List<String> listIpv6 = getIPV6AddressWIFI(ip);
                    if (listIpv6.size() == size)
                    {
                        if (attemptIpv6 <= 18)//30 tentativas, 1 minuto
                        {
                            attemptIpv6++;
                            mHandler.postDelayed(this, 3000);
                        }
                        else
                        {
                            attemptIpv6 = 0;
                            isRunningIPV6 = false;
                            mHandler.removeCallbacks(this);
                        }
                    }
                    else
                    {
                        if (listIpv6.size() >= 2)
                        {
                            Collections.sort(listIpv6);
                            Collections.reverse(listIpv6);
                        }
                        int index = 0;
                        mViewHolder.addressIpv6.setText("");
                        for (String ip : listIpv6) {
                            if (mViewHolder.ipv6Layout.getVisibility() != View.VISIBLE)
                                mViewHolder.ipv6Layout.setVisibility(View.VISIBLE);

                            mViewHolder.addressIpv6.append(ip);
                            if (index != listIpv6.size() - 1) {
                                mViewHolder.addressIpv6.append("\n");
                                index++;
                            }
                        }
                        mViewHolder.addressIpv6.setTextColor(c.getResources().getColor(R.color.colorAccent2));
                        attemptIpv6 = 0;
                        isRunningIPV6 = false;
                        mHandler.removeCallbacks(this);
                    }
                }
            }
        }
    }
    private class MobileDataStatus extends PhoneStateListener {
        private final Context c;
        private MobileDataStatus(Context c)
        {
            this.c = c;
        }
        @Override
        public void onDataConnectionStateChanged (int state, int networkType){
            super.onDataConnectionStateChanged(state, networkType);
            if(state == TelephonyManager.DATA_CONNECTED)
            {
                mViewHolder.connectionType.setText(getConnectionType(networkType,c));
                mViewHolder.typeConnectionName.setText(getNetworkName(networkType));
            }
        }
        private String getConnectionType(int networkType, Context context)
        {
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_IWLAN:
                case 19:  //LTE_CA
                    return "4G";
                default:
                    return context.getString(R.string.mask_unknown);
            }
        }
        private String getNetworkName(int networkType)
        {
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
                case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
                case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
                case TelephonyManager.NETWORK_TYPE_EHRPD: return "eHRPD";
                case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO rev. 0";
                case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO rev. A";
                case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO rev. B";
                case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
                case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
                case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
                case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
                case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
                case TelephonyManager.NETWORK_TYPE_IDEN: return "iDen";
                case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
                case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
                case TelephonyManager.NETWORK_TYPE_TD_SCDMA: return "TD SCDMA";
                case TelephonyManager.NETWORK_TYPE_IWLAN: return "IWLAN";
                case 19: return "LTE CA";//LTE_CA
                case TelephonyManager.NETWORK_TYPE_UNKNOWN: return c.getString(R.string.mask_unknown);
            }
            return c.getString(R.string.new_type_connection);
        }
    }
    public void loadExternalIp(final Context c)
    {
        threadExternalIp = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    final String ip = (externalIpTask = new LoadExternalIp()).execute().get(8000, TimeUnit.MILLISECONDS);
                    if (ip != null)
                    {
                        mViewHolder.buttonLoadIp.post(new Runnable()
                        {
                            @Override
                            public void run() {
                                mViewHolder.buttonLoadIp.setTextColor(c.getResources().getColor(R.color.colorAccent2));
                                mViewHolder.buttonLoadIp.setText(ip.toUpperCase());
                            }
                        });
                    }
                    else
                    {
                        mViewHolder.buttonLoadIp.post(new Runnable() {
                            @Override
                            public void run() {
                                mViewHolder.buttonLoadIp.setTextColor(c.getResources().getColor(R.color.colorRed));
                                mViewHolder.buttonLoadIp.setText(c.getString(R.string.erro_reload));
                                mViewHolder.buttonLoadIp.setClickable(true);
                                mViewHolder.buttonLoadIp.setFocusable(true);
                            }
                        });
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    mViewHolder.buttonLoadIp.post(new Runnable() {
                        @Override
                        public void run() {
                            mViewHolder.buttonLoadIp.setTextColor(c.getResources().getColor(R.color.colorRed));
                            mViewHolder.buttonLoadIp.setText(c.getString(R.string.time_out));
                            mViewHolder.buttonLoadIp.setClickable(true);
                            mViewHolder.buttonLoadIp.setFocusable(true);
                        }
                    });
                }
                catch(CancellationException ca)
                {
                    mViewHolder.buttonLoadIp.post(new Runnable() {
                        @Override
                        public void run() {
                            mViewHolder.buttonLoadIp.setTextColor(c.getResources().getColor(R.color.colorRed));
                            mViewHolder.buttonLoadIp.setText(c.getString(R.string.erro_reload));
                            mViewHolder.buttonLoadIp.setClickable(true);
                            mViewHolder.buttonLoadIp.setFocusable(true);
                        }
                    });
                }
            }
        });
        threadExternalIp.start();
    }
    private static class LoadExternalIp extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            BufferedReader in = null;
            try
            {
                URL externalIp = new URL("https://v4.ident.me/");
                in = new BufferedReader(new InputStreamReader(externalIp.openStream()));
                return in.readLine();
            }
            catch (IOException e)
            {
                try
                {
                    URL externalIp = new URL("https://v6.ident.me/");
                    in = new BufferedReader(new InputStreamReader(externalIp.openStream()));
                    return in.readLine();
                }
                catch (IOException ex)
                {
                    return null;
                }
            }
            finally
            {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}