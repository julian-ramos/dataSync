package com.ramos.julian.datasync;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class MainActivity extends Activity {
    Button syncButton, createButton;
    File file;
    FileOutputStream outputStream;
    PrintStream outPrint;
    String TAG="dataSync";
    GoogleApiClient mGoogleApiClient;

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        //This one is crucial
        //if the GoogleApiClient is istantiated but it is not connected it won't work
        mGoogleApiClient.connect();

        if (mGoogleApiClient.hasConnectedApi(Wearable.API)){
            Log.d(TAG,"Wearable API connected");
        }
        else{
            Log.d(TAG,"wearable API did not connect");
        }

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                syncButton = (Button) findViewById(R.id.button2);

                syncButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast toast;
                        toast=new Toast(getApplicationContext());
                        toast.makeText(getApplicationContext(),"Starting ",Toast.LENGTH_SHORT).show();

                        new Thread(new Runnable() {
                            public void run() {
                                SendTextFile();
                            }
                        }).start();


                    }
                });


//                createButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        file = new File(Environment.getExternalStorageDirectory()+"/randomFile.txt");
//                        try {
//                            outputStream = new FileOutputStream(file);
//                            outPrint = new PrintStream(outputStream);
//                            outPrint.println("THis is a random file");
//                            outPrint.println("Created on the wear device");
//                            outPrint.println("This is the end of the file");
//                            outPrint.close();
//                            outputStream.close();
//
//
//
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//                });




            }
        });




    }







    public void SendTextFile()
    {


        // Get folder for output
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath());
        if (!dir.exists()) {dir.mkdirs();} // Create folder if needed

        //Sending about 2.7MBs worked fine, 100k lines
        //6.9MBs worked fine 250K lines
        //8.3MBs worked fine 300k lines
        //11.1MBs failed 400k lines
        //Transfering 13.9MBs failed


        final File file = new File(dir, "randomFile.txt");


        if (file.exists()) file.delete();
        // Write a text file to external storage on the watch
        try {
            Date now = new Date();
            long nTime = now.getTime();
            FileOutputStream fOut = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fOut);
            Log.d(TAG,"Starting writing to file");
            for (int i=0;i<=300000;i++){
                ps.println(String.format(",%d",i)+"Time = "+Long.toString(nTime)); // A value that changes each time
            }

            ps.close();
            Log.d(TAG,"Done writing to file");


        } catch (Exception e) {
        }

        // Read the text file into a byte array
        FileInputStream fileInputStream = null;
        Log.d(TAG,"reading file");
        byte[] bFile = new byte[(int) file.length()];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();
        } catch (Exception e) {
        }
        Log.d(TAG,"File read preparing to send");

        // Create an Asset from the byte array, and send it via the DataApi

        Asset asset = Asset.createFromBytes(bFile);
        Log.d(TAG,"Asset created");
        PutDataMapRequest dataMap = PutDataMapRequest.create("/txt");
        Log.d(TAG,"Putting Asset");
        dataMap.getDataMap().putAsset("com.ramos.julian.dataSync.TXT", asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        Log.d(TAG,"Data request put");
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(mGoogleApiClient, request);
        if (mGoogleApiClient.hasConnectedApi(Wearable.API)){
            Log.d(TAG,"Wearable API connected");
        }
        else{
            Log.d(TAG,"wearable API did not connect");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
}
