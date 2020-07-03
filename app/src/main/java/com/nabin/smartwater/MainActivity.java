package com.nabin.smartwater;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public final static String MODULE_MAC = "00:18:E4:40:00:06";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    ConnectedThread btt = null;
    public Handler mHandler;

    float volume;

    Button boilWater;
    TextView textTemp;
    Switch boilerSwitch;
    TextView textEnterTemp;
    TextView textVolume;
    TextView textLevel;
    TextView textTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("[BLUETOOTH]", "Creating listeners");

        boilWater=findViewById(R.id.buttonBoil);
        textTemp=findViewById(R.id.textViewTemp);
        boilerSwitch=findViewById(R.id.switch1);
        textEnterTemp=findViewById(R.id.editTextEnterTemp);
        textLevel=findViewById(R.id.editTextWaterLevel);
        textVolume=findViewById(R.id.editTextVolume);
        textTimer=findViewById(R.id.textViewTimer);



        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if(!bta.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }else{
            initiateBluetoothProcess();
        }



        boilWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("[BLUETOOTH]", "Attempting to send data");
                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
                        String sendtxt = "BW";
                        btt.write(sendtxt.getBytes());
                        String enterTemp= textEnterTemp.getText().toString();
                        if (TextUtils.isEmpty(enterTemp)) {
                            textEnterTemp.setError("Required Field..");
                            return;
                        }
                        btt.write(enterTemp.getBytes());

                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()){

            //attempt to connect to bluetooth module
            BluetoothSocket tmp = null;
            mmDevice = bta.getRemoteDevice(MODULE_MAC);

            //create socket
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();
                Log.i("[BLUETOOTH]","Connected to: "+mmDevice.getName());
            }catch(IOException e){
                try{mmSocket.close();}catch(IOException c){return;}
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        System.out.println(txt);
                        stringAnalyse(txt);
                    }
                }
            };
            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
        }
    }

    public void stringAnalyse(String txt) {
        String disStr = null;
        int distance = 10;
        textTemp.setText(txt.substring(0,2));
        if (txt.substring(2, 5).equals("RON")) {
            boilerSwitch.setChecked(true);
        } else if (txt.substring(2, 5).equals("ROF")) {
            boilerSwitch.setChecked(false);
        }
        if (txt.length() >= 7) {
            disStr=txt.substring(5,7);
        }
        else if(txt.length()==6){
            disStr=txt.substring(5,6);
        }
        try{
            distance=Integer.parseInt(disStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }


        volume= (float) ((10-distance)*128.67);                    //our device has area of 128.67

        String disShow=String.valueOf(volume);
        textVolume.setText(disShow + " ml");

        if(volume==0)
            textLevel.setText("EMPTY");
        else if(volume<=300 && volume>0)
            textLevel.setText("LOW");
        else if (volume<=600 && volume >300)
            textLevel.setText("MEDIUM");
        else if (volume<=800 && volume >600)
            textLevel.setText("HIGH");
        else if (volume <1000 && volume >800)
            textLevel.setText("VERY HIGH");
        else
            textLevel.setText("FULL");
    }
}