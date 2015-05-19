// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btpollingtest;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by juksilve on 6.3.2015.
 */
public class TestDataFile {

    private final String fileNameStart = "BtPolData";
    private final String firstLine= "Os ,time ,battery ,rounds ,ConnecToFail ,CreateFail ,Connected ,ListenFail ,Connection ,ShakeOk ,ShakeFailed, shortest, longest, average \n";

    private File dbgFile;
    private OutputStream dbgFileOs;
    private Context context;

    public TestDataFile(Context Context){
        this.context = Context;
        Time t= new Time();
        t.setToNow();

        File path = this.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        String sFileName =  "/" + fileNameStart + t.yearDay + t.hour+ t.minute + t.second + ".txt";

        try {
            dbgFile = new File(path, sFileName);
            dbgFileOs = new FileOutputStream(dbgFile);
            dbgFileOs.write(firstLine.getBytes());
            dbgFileOs.flush();

            Log.d(fileNameStart, "File created:" + path + " ,filename : " + sFileName);
        }catch(Exception e){
            Log.d("FILE", "FileWriter, create file error, :" + e.toString());
        }
    }

    public void CloseFile(){
        try {
            if (dbgFile != null) {
                dbgFileOs.close();
                dbgFile.delete();
            }
        }catch (Exception e){
            Log.d(fileNameStart, "dbgFile close error :" + e.toString());
        }
    }

    public void WriteDebugline(int battery,long fullRoundCount,long socketConnecToFailCount,long socketCreateFailCount,long socketConnectedCount,long socketListenFailCount,long socketConnectionCount,long handShakeOkCount,long handShakeFailedCount, long shortest, long longest, long average ) {

        //"Os ,time ,battery ,Started ,got ,No services ,Peer err ,Service Err ,Add req Err ,reset counter \n";

        try {
            String dbgData = Build.VERSION.SDK_INT + " ," ;
            dbgData = dbgData  + System.currentTimeMillis() + " ,";
            dbgData = dbgData + battery + " ,";
            dbgData = dbgData + fullRoundCount + " ,";
            dbgData = dbgData + socketConnecToFailCount + " ,";
            dbgData = dbgData + socketCreateFailCount + " ,";
            dbgData = dbgData + socketConnectedCount + " ,";
            dbgData = dbgData + socketListenFailCount + " ,";
            dbgData = dbgData + socketConnectionCount + " ,";
            dbgData = dbgData + handShakeOkCount + " ,";
            dbgData = dbgData + handShakeFailedCount+ " ,";
            dbgData = dbgData + shortest + " ,";
            dbgData = dbgData + longest + " ,";
            dbgData = dbgData + average+ " \n";

            Log.d("FILE", "write: " + dbgData);
            dbgFileOs.write(dbgData.getBytes());
            dbgFileOs.flush();

        }catch(Exception e){
            Log.d("FILE", "dbgFile write error :" + e.toString());
        }
    }
}
