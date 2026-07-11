package com.example.xmldrawablepreview;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements PreviewServer.Listener {
    private DrawablePreviewView previewView;
    private PreviewServer server;
    private TextView status;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        previewView = new DrawablePreviewView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        status = new TextView(this);
        status.setText("POST raw XML to http://DEVICE_IP:8765/preview");
        root.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(label("Size"));
        SeekBar size = new SeekBar(this);
        size.setMax(400);
        size.setProgress(160);
        controls.addView(size, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        addStateToggle(controls, "Pressed", android.R.attr.state_pressed);
        addStateToggle(controls, "Focused", android.R.attr.state_focused);
        addStateToggle(controls, "Selected", android.R.attr.state_selected);
        addStateToggle(controls, "Enabled", android.R.attr.state_enabled, true);
        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(previewView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                previewView.setPreviewSize(Math.max(24, progress));
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });

        server = new PreviewServer(8765, this);
        try {
            server.start();
        } catch (IOException e) {
            Toast.makeText(this, "Server failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        return view;
    }

    private void addStateToggle(LinearLayout controls, String text, int state) {
        addStateToggle(controls, text, state, false);
    }

    private void addStateToggle(LinearLayout controls, String text, final int state, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(text);
        box.setChecked(checked);
        previewView.setState(state, checked);
        box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                previewView.setState(state, isChecked);
            }
        });
        controls.addView(box);
    }

    public void onXmlReceived(final byte[] xml) {
        runOnUiThread(new Runnable() {
            public void run() {
                previewView.setXml(xml);
                status.setText("Loaded " + xml.length + " bytes from POST /preview");
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }
}
