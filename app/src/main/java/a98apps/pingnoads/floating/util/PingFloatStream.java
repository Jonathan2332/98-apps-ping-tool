package a98apps.pingnoads.floating.util;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a98apps.pingnoads.R;
import a98apps.pingnoads.constants.PingConstants;
import a98apps.pingnoads.floating.window.PingFloatWindow;
import a98apps.pingnoads.network.NetworkStatus;
import a98apps.pingnoads.util.SecurityPreferences;

class PingFloatStream extends Thread
{
    private final InputStream stream;
    private final Context activity;
    private boolean running;
    private int packet;
    private int packetLocal = 1;//icmp_seq inicia no 1
    private int totalPacketsLoss;
    private boolean packetLostOrder = false;
    private final SecurityPreferences mPreferences;
    private int countReachable = 5;//TIMEOUT
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int countAttempt = 0;
    private final int timeoutInterval;
    private final PingFloat ping;
    private final PingFloatWindow floatPing;
    private boolean flagStop;

    private int getPacket() {
        return packet;
    }

    private void setPacket(int packet) {
        this.packet = packet;
    }

    private boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    PingFloatStream(InputStream is, Context activity, SecurityPreferences p, PingFloatWindow tab, PingFloat ping)
    {
        setRunning(true);
        this.stream = is;
        this.activity = activity;
        this.mPreferences = p;
        this.ping = ping;
        this.floatPing = tab;
        if(Double.parseDouble(p.getSetting(PingConstants.INTERVAL)) >= countReachable)
            this.timeoutInterval = Integer.parseInt(p.getSetting(PingConstants.INTERVAL))+3;
        else
            this.timeoutInterval = countReachable;
        this.start();
    }
    @Override
    public void run()
    {
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            mHandler.postDelayed(TimeoutRunnable, 1000);
            Pattern pattern;
            Matcher matcher;
            String line;
            String enablePacketLoss = mPreferences.getSetting(PingConstants.ENABLE_PACKETLOSS);
            String enablePingOnly = mPreferences.getSetting(PingConstants.ENABLE_PINGONLY);
            while ((line = br.readLine()) != null && isRunning())
            {
                countReachable = -1;
                pattern = Pattern.compile("icmp_seq=(\\d*)");
                matcher = pattern.matcher(line);
                if (matcher.find())
                {
                    if(!line.contains("Destination Host Unreachable"))
                    {
                        if(!line.contains("Destination Net Unreachable"))
                        {
                            setPacket(Integer.parseInt(matcher.group(1)));
                            if (packetLocal == getPacket())
                            {
                                packetLocal = getPacket() + 1;
                                if (!line.contains("(DUP!)"))
                                {
                                    if(line.contains("time"))
                                        resultPing(line.replaceAll("time", "<b>time</b>"),false);
                                    else if(line.contains("icmp_seq"))
                                        resultPing(line.replaceAll("icmp_seq", "<b>icmp_seq</b>"),false);
                                    else
                                        resultPing(line,false);
                                }
                                else
                                    resultPing("<strike>" + line + "</strike>", false);
                            }
                            else if (packetLocal > getPacket())
                            {
                                packetLostOrder = true;
                                if(totalPacketsLoss >= 1) totalPacketsLoss--;
                                resultPing("<strike>" + line + "</strike>", true);//true to update the total packet loss
                            }
                            else
                            {
                                totalPacketsLoss += getPacket() - packetLocal;
                                int lostPacket = getPacket() - packetLocal;
                                packetLocal = getPacket() + 1;
                                if (enablePacketLoss.equals(PingConstants.YES))
                                {
                                    if (lostPacket == 1)
                                    {
                                        resultPing("<font color='red'>" + lostPacket + activity.getString(R.string.lost_packet) + "</font>", true);
                                        if(line.contains("time"))
                                            resultPing(line.replaceAll("time", "<b>time</b>"),false);
                                        else if(line.contains("icmp_seq"))
                                            resultPing(line.replaceAll("icmp_seq", "<b>icmp_seq</b>"),false);
                                        else
                                            resultPing(line,false);
                                    }
                                    else
                                    {
                                        resultPing("<font color='red'>" + lostPacket + activity.getString(R.string.lost_packets) + "</font>", true);
                                        if(line.contains("time"))
                                            resultPing(line.replaceAll("time", "<b>time</b>"),false);
                                        else if(line.contains("icmp_seq"))
                                            resultPing(line.replaceAll("icmp_seq", "<b>icmp_seq</b>"),false);
                                        else
                                            resultPing(line,false);
                                    }
                                }
                                else
                                {
                                    if(line.contains("time"))
                                        resultPing(line.replaceAll("time", "<b>time</b>"),false);
                                    else if(line.contains("icmp_seq"))
                                        resultPing(line.replaceAll("icmp_seq", "<b>icmp_seq</b>"),false);
                                    else
                                        resultPing(line,false);
                                }
                            }
                        }
                        else
                        {
                            ping.stopResult("<font color='red'><b>" + activity.getString(R.string.internet_unavaiable) + "</b></font>");
                            setRunning(false);//sai do loop
                        }
                    }
                    else
                    {
                        ping.stopResult("<font color='red'><b>" + activity.getString(R.string.host_unreachable) + "</b></font>");
                        setRunning(false);//sai do loop
                    }
                }
                else
                {
                    if(enablePingOnly.equals(PingConstants.NO))
                    {
                        if (line.contains("bytes") || line.contains("statistics") || line.contains("rtt")) {
                            resultPing(line, false);
                        }
                        else if(line.contains("transmitted"))
                        {
                            pattern = Pattern.compile("(\\d*) packets transmitted");
                            matcher = pattern.matcher(line);
                            if (matcher.find())
                            {
                                if(getPacket() != Integer.parseInt(matcher.group(1))) {
                                    totalPacketsLoss += Integer.parseInt(matcher.group(1)) - packetLocal+1;
                                    resultPing(line, true);//update total packet loss
                                }
                                else resultPing(line, false);
                            }
                            flagStop = true;
                        }
                    }
                    else
                    {
                        if(line.contains("statistics")) {
                            resultPing("<font color='#8bc34a'><b>" + activity.getString(R.string.count_finished) + "</b></font>", false);
                            flagStop = true;
                            setRunning(false);
                        }
                    }
                }
            }
            mHandler.removeCallbacks(TimeoutRunnable);
            if (packetLostOrder) {
                resultPing("<font color='#ff9243'><b>" + activity.getString(R.string.packet_recounted) + "</b></font>", false);
                packetLostOrder = false;
            }
            if(flagStop && !ping.isSignalStop()) { ping.stopResult(null); ping.setSignalStop(false); }
            stopStream();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }
    private void resultPing(final String result, final boolean lostPacket)
    {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                floatPing.addItems(result);
                if(lostPacket)
                    floatPing.addItems("<font color='red'>" + totalPacketsLoss + activity.getString(R.string.total_loss)+ "</font>");
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isDeviceInDozeMode() {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        return pm.isDeviceIdleMode();
    }
    private final Runnable TimeoutRunnable = new Runnable()
    {
        public void run()
        {
            if(isRunning() && countReachable == -1)
            {
                countReachable = timeoutInterval;
                countAttempt = 0;
                mHandler.postDelayed(TimeoutRunnable, 1000);
            }
            else
            {
                if(countReachable >= 1 && isRunning())
                {
                    countReachable--;
                    mHandler.postDelayed(TimeoutRunnable, 1000);
                }
                else if(countReachable == 0 && isRunning())
                {
                    if (countAttempt >= 10)
                    {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isDeviceInDozeMode()) {
                            ping.stopResult("<font color='red'><b>" + activity.getString(R.string.doze_mode) + "</b></font>");
                            countAttempt = 0;
                        }
                        else if(NetworkStatus.getConnectionType(activity.getApplicationContext(),true).equals(activity.getString(R.string.no_connection)))
                        {
                            ping.stopResult("<font color='red'><b>" + activity.getString(R.string.no_connection) + "</b></font>");
                            countAttempt = 0;
                        }
                        else
                        {
                            ping.stopResult("<font color='red'><b>" + activity.getString(R.string.host_unreachable) + "</b></font>");
                            countAttempt = 0;
                        }
                    }
                    else
                    {
                        resultPing(activity.getString(R.string.request_timedout), false);
                        countAttempt++;
                        mHandler.postDelayed(TimeoutRunnable, 4000);
                    }
                }
            }
        }
    };
    void stopStream()
    {
        setRunning(false);
        this.flagStop = false;
        mHandler.removeCallbacks(TimeoutRunnable);
        this.interrupt();
    }
}
