package com.ayman.opencv;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ArduinoSerial {
    private final Context mContext;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;
    private byte[] data;

    private UsbSerialInterface.UsbReadCallback mCallback;

    private final String ACTION_USB_PERMISSION = "com.ayman.arduinousb.USB_PERMISSION";
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Toast.makeText(mContext, "Serial Connection Opened!", Toast.LENGTH_LONG).show();

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                begin();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                stop();
            }
        }

        ;
    };


    public ArduinoSerial(Context context) {
//        UsbSerialInterface.UsbReadCallback callback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
//            @Override
//            public void onReceivedData(byte[] data) {
//                if(data != null && data.length > 0) {
//                    Log.d("ArduinoData", String.valueOf(data[0]));
//                }
////            String dataString = null;
////            try {
////                dataString = new String(data, "UTF-8");
////                dataString.concat("/n");
////                Log.d("ArduinoData", dataString);
////            } catch (UnsupportedEncodingException e) {
////                e.printStackTrace();
////            }
//
//
//            }
//        };
        this(context, new UsbSerialInterface.UsbReadCallback() {
            @Override
            public void onReceivedData(byte[] data) {
                if (data != null && data.length > 0) {
                    for (byte element : data)
                        Log.d("ArduinoData", String.valueOf(element));
                }
            }
        });
    }

    public ArduinoSerial(Context context, UsbSerialInterface.UsbReadCallback callback) {
        mContext = context;
        usbManager = (UsbManager) context.getSystemService(Activity.USB_SERVICE);
        mCallback = callback;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(broadcastReceiver, filter);
        data = new byte[2];
    }

    public void begin() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                Log.d("DEVICE_ID", deviceVID + "");
                Toast.makeText(mContext, deviceVID + "", Toast.LENGTH_LONG).show();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public void stop() {
        if (serialPort != null) {
            serialPort.close();
            Toast.makeText(mContext, "Serial Connection Closed!", Toast.LENGTH_LONG).show();
        }
    }

    public void send(byte buffer[]) {
        if (serialPort != null)
            serialPort.write(buffer);
    }

    public void send(int x, int type) {
        type <<= 6;
        data[0] = (byte) (x & 0x003f | type);
        data[1] = (byte) ((x >> 6) & 0x003f | type);
        send(data);
    }

    public void sendInt(int x) {
        byte data[] = new byte[]{(byte) x, (byte) (x >> 8)};
        send(data);
    }

    public void send(int... numbers) {
        if (numbers.length > 3)
            return;
        for (int i = 0; i < numbers.length; i += 2) {
            data[i] = (byte) (numbers[i] | i / 2);
            data[i + 1] = (byte) ((numbers[i] >> 8) | i / 2);
        }
        send(data);
    }

    public void destroy() {
        mContext.unregisterReceiver(broadcastReceiver);
    }
}
