package org.thaliproject.p2p.btpollingtest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by juksilve on 8.5.2015.
 */
public class BtPollingService extends Service implements BluetoothBase.BluetoothStatusChanged {

    BtPollingService that = this;

    static final public String POLLING_VALUES = "org.thaliproject.p2p.btpollingtest.POLLING_VALUES";
    static final public String POLLING_MESSAGE = "org.thaliproject.p2p.btpollingtest.POLLING_MESSAGE ";

    BluetoothBase mBluetoothBase = null;
    BTConnectToThread mBTConnectToThread = null;
    BTListenerThread mBTListenerThread = null;

    IntentFilter mfilter = null;
    BroadcastReceiver mReceiver = null;
    TestDataFile mTestDataFile = null;
    int lastChargePercent = -1;

    long fullRoundCount = 0;
    long socketConnecToFailCount = 0;
    long socketCreateFailCount = 0;
    long socketConnectedCount = 0;
    long socketListenFailCount = 0;
    long socketConnectionCount = 0;
    long handShakeOkCount = 0;
    long handShakeFailedCount = 0;

    long startQueryTimeStamp = 0;
    long waitTimeStartStamp = 0;

    long lastSaveTimeStamp = 0;

    List<Long> timeStampList = new ArrayList<Long>();

     public void DoSaveDebugDataNow() {

         long shortest = -1;
         long longest = -1;
         long average = -1;
         int count = timeStampList.size();
         if (count > 0) {
             shortest = timeStampList.get(0);
             longest = timeStampList.get(0);
             average = timeStampList.get(0);

             for (int i = 1; i < count; i++) {
                 average = average + timeStampList.get(i);

                 if (shortest > timeStampList.get(i)) {
                     shortest = timeStampList.get(i);
                 }
                 if (longest < timeStampList.get(i)) {
                     longest = timeStampList.get(i);
                 }
             }

             average = (average / count);
         }

         if (mTestDataFile != null) {
             mTestDataFile.WriteDebugline(lastChargePercent, fullRoundCount, socketConnecToFailCount, socketCreateFailCount, socketConnectedCount, socketListenFailCount, socketConnectionCount, handShakeOkCount, handShakeFailedCount, shortest, longest, average);
         }
         timeStampList.clear();
     }



    final CountDownTimer PollingTimer = new CountDownTimer(1000, 500) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            DoOneRound();
        }
    };

    String latsDbgString = "";

    long roundsCount(){
        return fullRoundCount;
    }

    String getLastDbgString() {

        String ret = "Rounds: " + fullRoundCount + ", last charge: " + lastChargePercent + "%\n";
        ret =  ret + "Create fail: " + socketCreateFailCount+ "Connect fail: " + socketConnecToFailCount + "\n";
        ret =  ret + "last: " + latsDbgString + "\n";

        return ret;
    }


    private final IBinder mBinder = new MyLocalBinder();

    public class MyLocalBinder extends Binder {
        BtPollingService getService() {
            return BtPollingService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        print_line("SearchService","onStartCommand rounds so far :");
        Start();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        print_line("SearchService", "onDestroy");
        super.onDestroy();
    }

    boolean isRunnuing(){
        boolean ret = false;
     /*   if(mWifiBase != null){
            ret = true;
        }
       */ return ret;
    }

    public void Start() {

        mTestDataFile = new TestDataFile(this);
        //SaveDataTimeOutTimer.start();

        mfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mReceiver = new PowerConnectionReceiver();
        registerReceiver(mReceiver, mfilter);

        if(mBluetoothBase != null){
            mBluetoothBase.Stop();
            mBluetoothBase = null;
        }
        mBluetoothBase = new BluetoothBase(this, this);
        Boolean btOk = mBluetoothBase.Start();

        if (!btOk) {
            print_line("", "BT not available available: ");
        } else if (mBluetoothBase.isBluetoothEnabled()) {
            print_line("", "All stuff available and enabled");
           startAll();
        }else{
            print_line("", "BT is off, please turn BT on");
        }
    }

    public void Stop() {
        stopAll();

        if(mReceiver != null){
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mBluetoothBase != null) {
            mBluetoothBase.Stop();
            mBluetoothBase = null;
        }
        if(mTestDataFile != null) {
            print_line("SearchService","Closing File");
            mTestDataFile.CloseFile();
            mTestDataFile = null;
        }
    }

    public void stopAll(){
        PollingTimer.cancel();

        if (mBTListenerThread != null) {
            mBTListenerThread.Stop();
            mBTListenerThread = null;
        }

        if (mBTConnectToThread != null) {
            mBTConnectToThread.Stop();
            mBTConnectToThread = null;
        }
    }

    public void startAll(){
        stopAll();

        if (mBluetoothBase != null) {
            mBTListenerThread = new BTListenerThread(this,mBluetoothBase.getAdapter());
            mBTListenerThread.start();
        }
        lastSaveTimeStamp  = System.currentTimeMillis();
        DoOneRound();
    }

    public void DoOneRound(){

        long nowtimme = System.currentTimeMillis();
        if((nowtimme - lastSaveTimeStamp) > 600000){
            lastSaveTimeStamp = System.currentTimeMillis();
            DoSaveDebugDataNow();
        }

        if (mBTConnectToThread != null) {
            mBTConnectToThread.Stop();
            mBTConnectToThread = null;
        }

        String AddressLine = "AA:BB:CC:DD:EE:FF";
        BluetoothDevice device = mBluetoothBase.getRemoteDevice(AddressLine);

        waitTimeStartStamp = System.currentTimeMillis();
        startQueryTimeStamp = System.currentTimeMillis();
        fullRoundCount =  fullRoundCount + 1;

        mBTConnectToThread = new BTConnectToThread(that, device);
        mBTConnectToThread.start();
    }

    @Override
    public void CreateSocketFailed(String reason) {
        socketCreateFailCount = socketCreateFailCount + 1;
    }

    @Override
    public void ConnectionFailed(String reason) {

        socketConnecToFailCount = socketConnecToFailCount + 1;

        long QueryTime = (System.currentTimeMillis() - startQueryTimeStamp);
        timeStampList.add(QueryTime);
        if(QueryTime < 3000){
            PollingTimer.start();
        }else{
            DoOneRound();
        }

        print_line("CON","Count : " + socketConnecToFailCount +  "failed with reason : " +reason );
    }

    @Override
    public void Connected(BluetoothSocket socket) {
        socketConnectedCount = socketConnectedCount + 1;
    }

    @Override
    public void ListeningFailed(String reason) {
        socketListenFailCount = socketListenFailCount + 1;

        if (mBTListenerThread != null) {
            mBTListenerThread.Stop();
            mBTListenerThread = null;
        }
        if (mBluetoothBase != null) {
            mBTListenerThread = new BTListenerThread(this,mBluetoothBase.getAdapter());
            mBTListenerThread.start();
        }
    }


    @Override
    public void GotConnection(BluetoothSocket socket) {
        socketConnectionCount = socketConnectionCount + 1;
    }

    @Override
    public void BluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.SCAN_MODE_NONE) {
            print_line("BT", "Bluetooth DISABLED, stopping");
            stopAll();
        } else if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE
                || state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            startAll();
        }
    }

    public void print_line(String who, String line) {
        latsDbgString = who + " : " + line;
        Log.i("BtTestMaa" + who, line);
    }

    public class PowerConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //     print_line("", "Action : " + intent.getAction());

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);

            lastChargePercent = (level*100)/scale;

            String message = "Battery charge: " + lastChargePercent + " %";
        }
    }
}