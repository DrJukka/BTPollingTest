// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btpollingtest;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by juksilve on 12.3.2015.
 */
public class BTConnectToThread extends Thread {

    private final BluetoothBase.BluetoothStatusChanged callback;
    private final BluetoothSocket mSocket;

    final UUID BtUUID     = UUID.fromString("ED01E22D-19DD-4B79-81BA-444E9B6D89BF");

    public BTConnectToThread(BluetoothBase.BluetoothStatusChanged Callback, BluetoothDevice device) {
        callback = Callback;
        BluetoothSocket tmp = null;
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(BtUUID);
        } catch (IOException e) {
            print_line("createInsecure.. failed: " + e.toString());
            callback.CreateSocketFailed(e.toString());
        }
        mSocket = tmp;
    }

    public void run() {
        print_line("Starting to connect");
        if(mSocket != null && callback != null) {
            try {
                mSocket.connect();
                //return success
                callback.Connected(mSocket);
            } catch (IOException e) {
                print_line("socket connect failed: " + e.toString());
                try {
                    mSocket.close();
                } catch (IOException ee) {
                    print_line("closing socket 2 failed: " + ee.toString());
                }
                callback.ConnectionFailed(e.toString());
            }
        }
    }

    private void print_line(String message) {
       //    Log.d("BTConnectToThread", "BTConnectToThread: " + message);
    }

    public void Stop() {
        try {
            if(mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            print_line("closing socket failed: " + e.toString());
        }
    }
}
