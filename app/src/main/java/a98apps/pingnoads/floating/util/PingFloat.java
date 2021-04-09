package a98apps.pingnoads.floating.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;
import a98apps.pingnoads.floating.window.PingFloatWindow;
import a98apps.pingnoads.network.NetworkStatus;
import a98apps.pingnoads.util.SecurityPreferences;

public class PingFloat extends Thread {

    private String url;
    private Context activity;
    private boolean running;
    private Process proc = null;
    private PingFloatStream terminalInput;
    private final SecurityPreferences mSecurityPreferences;
    private final String enablePingOnly;
    private NetTask netTask;
    private CheckAccess checkTask;
    private final String settingCount;
    private final String packetBytes;
    private boolean hasIpv4 = false;
    private boolean hasIpv6 = false;
    private final PingFloatWindow floatPing;
    private boolean signalStop = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    boolean isSignalStop() {
        return signalStop;
    }

    void setSignalStop(boolean signalStop) {
        this.signalStop = signalStop;
    }

    private boolean isHasIpv4() {
        return hasIpv4;
    }

    private void setHasIpv4(boolean hasIpv4) {
        this.hasIpv4 = hasIpv4;
    }

    private boolean isHasIpv6() {
        return hasIpv6;
    }

    private void setHasIpv6(boolean hasIpv6) {
        this.hasIpv6 = hasIpv6;
    }

    private void setUrl(String url) {
        this.url = url;
    }

    private void setActivity(Context activity) {
        this.activity = activity;
    }

    private Context getActivity() {
        return activity;
    }

    private boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    private String getUrl() {
        return url;
    }

    public PingFloat(Context context, String url, SecurityPreferences security, PingFloatWindow floatPing)
    {
        setActivity(context);
        setUrl(url);
        setRunning(true);
        this.floatPing = floatPing;
        this.mSecurityPreferences = security;
        this.enablePingOnly = mSecurityPreferences.getSetting(PingConstants.ENABLE_PINGONLY);
        this.settingCount = mSecurityPreferences.getSetting(PingConstants.COUNT_PING);
        this.packetBytes = mSecurityPreferences.getSetting(PingConstants.PACKET_BYTES);
        this.start();
    }

    @Override
    public void run()
    {
        try
        {
            String[] commands = null;
            boolean isLocal;
            try
            {
                isLocal = isLocalAddress(InetAddress.getByName(getUrl()));
            }
            catch (UnknownHostException u)
            {
                isLocal = false;
            }
            if(!NetworkStatus.getConnectionType(getActivity(), true).equals(getActivity().getString(R.string.no_connection)) && !isLocal)
            {
                if ((checkTask = new CheckAccess(this)).execute().get())
                {
                    if (mSecurityPreferences.getSetting(PingConstants.PING_MODE).equals(PingConstants.IPV4_ONLY))
                    {
                        if (isHasIpv4())
                        {
                            if (settingCount.equals(PingConstants.COUNT_UNLIMITED))
                            {
                                if (getUrl() != null)
                                {
                                    if (getUrl().equals("::1"))
                                        commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                                    else
                                        commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                                }
                                else
                                    this.interrupt();
                            }
                            else if (getUrl() != null)
                            {
                                if (getUrl().equals("::1"))
                                    commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                                else
                                    commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                            }
                            else
                                this.interrupt();
                        }
                        else
                        {
                            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                                stopResult("<font color='red'><b>" + getActivity().getString(R.string.ipv4_off) + "</b></font>");
                            }
                            else
                            {
                                stopResult("<font color='red'><b>" + getActivity().getString(R.string.ipv4off_pie) + "</b></font>");
                            }
                        }
                    }
                    else {

                        try
                        {
                            String address = (netTask = new NetTask()).execute(getUrl()).get(10000, TimeUnit.MILLISECONDS);//To prevent freeze this thread when is cancelled
                            if (address != null)
                            {
                                if(address.indexOf(':')<0) {//IPV4
                                    if (settingCount.equals(PingConstants.COUNT_UNLIMITED))
                                        commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                                    else
                                        commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                                }
                                else//ipv6
                                {
                                    if (settingCount.equals(PingConstants.COUNT_UNLIMITED))
                                        commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                                    else
                                        commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                                }
                            }
                            else
                            {
                                if (NetworkStatus.getConnectionType(activity, true).equals(activity.getString(R.string.no_connection)))
                                    stopResult("<font color='red'><b>" + getActivity().getString(R.string.no_connection) + "</b></font>");
                                else
                                    stopResult("<font color='red'><b>" + getActivity().getString(R.string.unknown_host) + "</b></font>");
                            }

                        }
                        catch (TimeoutException t)
                        {
                            stopResult("<font color='red'><b>" + getActivity().getString(R.string.internet_unavaiable) + "</b></font>");
                        }
                        catch (ExecutionException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                else
                    stopResult("<font color='red'><b>" + getActivity().getString(R.string.internet_unavaiable) + "</b></font>");
            }
            else
            {
                if(getUrl() != null)
                {
                    if (getUrl().indexOf(':') < 0)
                    {
                        if (settingCount.equals(PingConstants.COUNT_UNLIMITED))
                            commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                        else
                            commands = new String[]{"ping", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                    }
                    else//ipv6
                    {
                        if (settingCount.equals(PingConstants.COUNT_UNLIMITED))
                            commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), getUrl()};
                        else
                            commands = new String[]{"ping6", "-s", packetBytes, "-i", mSecurityPreferences.getSetting(PingConstants.INTERVAL), "-c", settingCount, getUrl()};
                    }
                }
                else
                    this.interrupt();
            }
            if(commands != null)
            {
                if(proc == null)
                {
                    ProcessBuilder builder = new ProcessBuilder(commands);
                    proc = builder.start();

                    terminalInput = new PingFloatStream(proc.getInputStream(), getActivity(), mSecurityPreferences, floatPing, this);

                    proc.waitFor();//freeze the thread
                    int exit = proc.exitValue();
                    if (exit >= 0 && !isRunning())
                    {
                        this.interrupt();
                    }
                    else if(exit == 0 || exit == 1)//0 = success, 1 = warning, 2 = error
                    {
                        this.interrupt();
                    }
                    else
                    {
                        String connectionType = NetworkStatus.getConnectionType(getActivity(), true);
                        if (connectionType.equals(getActivity().getString(R.string.no_connection)))
                            stopResult("<font color='red'><b>" + getActivity().getString(R.string.no_connection) + "</b></font>");

                        else if(!(checkTask = new CheckAccess(this)).execute().get())
                            stopResult("<font color='red'><b>" + getActivity().getString(R.string.internet_unavaiable) + "</b></font>");

                        else
                        {
                            InputStreamReader isr = new InputStreamReader(proc.getErrorStream());//Pega os erros gerado pelo terminal, caso o usuário tenha digitado algo errado, como exemplo: a URL
                            BufferedReader br = new BufferedReader(isr);
                            String result;
                            if ((result = br.readLine()) != null)
                            {
                                if (result.contains("-b"))
                                {
                                    stopResult("<font color='red'><b>" + getActivity().getString(R.string.invalid_url) + "</b></font>");
                                }
                                else if(result.contains("unknown")) {
                                    stopResult("<font color='red'><b>" + getActivity().getString(R.string.contains_unknown) + getUrl() + "</b></font>");
                                }
                                else if(result.contains("connect:"))
                                {
                                    if(!connectionType.equals(getActivity().getString(R.string.no_connection)) && (checkTask = new CheckAccess(this)).execute().get())
                                    {
                                        if(!hasIpv4)
                                            stopResult("<font color='red'><b>" + getActivity().getString(R.string.no_ipv4) + "</b></font>");
                                        else if(!isHasIpv6())
                                            stopResult("<font color='red'><b>" + getActivity().getString(R.string.no_ipv6) + "</b></font>");
                                    }
                                    else
                                        stopResult("<font color='red'><b>" + getActivity().getString(R.string.internet_unavaiable) + "</b></font>");
                                }
                                else
                                {
                                    stopResult("<font color='red'><b>" + result + "</b></font>");
                                }
                            }
                            else {
                                stopResult("<font color='red'><b>" + getActivity().getString(R.string.no_support) + "</b></font>");
                            }
                        }
                    }
                }
                else if(getPid(proc) != -1)
                    stopPing();
                else
                    this.interrupt();//proc != null
            }
            else this.interrupt();//commandos == null
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            stopPing();//Que coisa feia KKKKKKKKK
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
    }
    private void stopPing()
    {
        setRunning(false);
        setHasIpv4(false);
        setHasIpv6(false);
        setUrl(null);
        setActivity(null);
        if(checkTask != null) checkTask.cancel(true);
        if(netTask != null) netTask.cancel(true);
        int pid = getPid(proc);
        if(enablePingOnly != null)
        {
            if (enablePingOnly.equals(PingConstants.NO))
            {
                if (pid != -1) {
                    android.os.Process.sendSignal(pid, 2);//SIGINT
                    setSignalStop(true);
                }
            }
            else
            {
                if (terminalInput != null)
                    terminalInput.stopStream();

                if (pid != -1)
                    android.os.Process.killProcess(pid);
            }
        }
        else
        {
            if (terminalInput != null)
                terminalInput.stopStream();

            if (pid != -1)
                android.os.Process.killProcess(pid);
        }
    }
    void stopResult(final String text)
    {
        if(text != null)
        {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        floatPing.addItems(text);
                        floatPing.stopPing();
                    }
                });
        }
        else
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    floatPing.stopPing();
                }
            });
        }
    }
    private int getPid(Process p)
    {
        int pid;
        try
        {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(p);
            f.setAccessible(false);
        }
        catch (Exception e)
        {
            pid = -1;
        }
        return pid;
    }
    private static class NetTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                return InetAddress.getByName(params[0]).getHostAddress();
            }
            catch (UnknownHostException e)
            {
                return null;
            }
        }
    }
    private static class CheckAccess extends AsyncTask<Boolean, Void, Boolean>
    {
        private final PingFloat ping;

        private CheckAccess(PingFloat p) {
            this.ping = p;
        }
        @Override
        protected Boolean doInBackground(Boolean... params)
        {
            try
            {
                Socket sock = new Socket();
                SocketAddress sockAddress = new InetSocketAddress("8.8.4.4", 53);

                sock.connect(sockAddress, 9000);
                sock.close();
                ping.setHasIpv4(true);

                return true;
            }
            catch (IOException e)
            {
                try
                {
                    Socket sock = new Socket();
                    SocketAddress sockAddress = new InetSocketAddress("2001:4860:4860::8844", 53);//ipv6 google

                    sock.connect(sockAddress, 9000);
                    sock.close();
                    ping.setHasIpv6(true);

                    return true;
                } catch (IOException e1) {
                    return false;
                }
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
        }
    }
    private boolean isLocalAddress(InetAddress addr)
    {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;
        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        }
        catch (SocketException e) {
            return false;
        }
    }
}