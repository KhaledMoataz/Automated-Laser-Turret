package com.ayman.opencv;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialInterface;

public class DebugActivity extends AppCompatActivity implements UsbSerialInterface.UsbReadCallback {

    private ArduinoSerial mSerial;
    private TextView mReceivedDataView;
    private EditText mNumberToSend;
    private Button mSendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        mSerial = new ArduinoSerial(this, this);
        mReceivedDataView = findViewById(R.id.received_data_view);
        mNumberToSend = findViewById(R.id.number_to_send);
        mSendButton = findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int number = Integer.parseInt(mNumberToSend.getText().toString());
                mSerial.sendInt(number);
            }
        });
    }

    @Override
    public void onReceivedData(byte[] data) {
        if (data != null) {
            StringBuilder builder = new StringBuilder();
            for (byte element : data) {
                builder.append(element);
                builder.append('\n');
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mReceivedDataView.setText(mReceivedDataView.getText() + builder.toString());
                }
            });
        }
    }
}
