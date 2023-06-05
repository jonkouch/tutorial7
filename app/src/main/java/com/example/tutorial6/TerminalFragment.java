package com.example.tutorial6;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.O)
public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    LineChart mpLineChart;
    LineDataSet lineDataSet1;
    LineDataSet lineDataSet2;
    LineDataSet lineDataSet3;
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;
    String activityType;
    String csvName;
    String stepNumber;
    String estimatedSteps;
    int chartIndex;
    boolean start_flag=false;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    boolean firstReceive = true;
    boolean no_action_flag = true;
    boolean stopped = false;
    View view;

    float startTime;
    String directoryPath = "/sdcard/csv_dir/";
    List<String> filenames = new ArrayList<>();
    File directory = new File(directoryPath);
    File[] files = directory.listFiles();

    String row1[];
    String row2[];
    String row3[];
    String row4[];
    String row5[];
    String row6[];
    ArrayList<String[]> csv_data;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));



        files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if(!filenames.contains(file.getName()))
                        filenames.add(file.getName());
                }
            }
        }

        Spinner spinner = view.findViewById(R.id.spinner2);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.activity_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activityType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case when no item is selected
            }
        });

        EditText textButton = view.findViewById(R.id.csv_name);
        EditText stepsButton = (EditText) view.findViewById(R.id.step_number);

        Button start = view.findViewById(R.id.start_btn);


        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                directory = new File(directoryPath);
                files = directory.listFiles();
                csvName = textButton.getText().toString();
                stepNumber = stepsButton.getText().toString();

                if(!no_action_flag){
                    Toast.makeText(service.getApplicationContext(), "You cannot start a recording before saving or deleting the previous one!", Toast.LENGTH_SHORT).show();
                }

                else if(csvName.matches("") || stepNumber.matches("")) {
                    Toast.makeText(service.getApplicationContext(), "You need to fill the fields above!", Toast.LENGTH_SHORT).show();
                }

                else if(filenames.contains(csvName+".csv")){
                    Toast.makeText(service.getApplicationContext(), "This file name is taken, please choose a different name!", Toast.LENGTH_SHORT).show();
                }
                else {
                    csv_data = new ArrayList<>();
                    filenames.add(csvName + ".csv");
                    no_action_flag = false;
                    firstReceive = true;
                    stopped = false;
                    row1 = new String[]{"NAME:", csvName};
                    row2 = new String[]{"EXPERIMENT TIME:", (String) dtf.format(LocalDateTime.now())};
                    row3 = new String[]{"ACTIVITY TYPE:", activityType};
                    row4 = new String[]{"COUNT OF ACTUAL STEPS:", stepNumber};
                    row6 = new String[]{"Time[sec]", "Acceleration"};

                    start_flag = true;
                    chartIndex = 0;
                }
            }
        });


        Button reset = view.findViewById(R.id.reset_btn);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(no_action_flag){
                    Toast.makeText(service.getApplicationContext(), "What are you trying to reset?", Toast.LENGTH_SHORT).show();
                }
                else if(!stopped) {
                    Toast.makeText(service.getApplicationContext(), "Recording must be stopped before resetting", Toast.LENGTH_SHORT).show();
                }
                else {
                    start_flag = false;
                    stopped = false;
                    no_action_flag = true;
                    csv_data.clear();
                    filenames.remove(csvName+".csv");

                    LineData data = mpLineChart.getData();
                    ILineDataSet set = data.getDataSetByIndex(0);
                    data.getDataSetByIndex(0);
                    while (set.removeLast()) {
                    }
//                    set = data.getDataSetByIndex(1);
//                    while (set.removeLast()) {
//                    }
//                    set = data.getDataSetByIndex(2);
//                    while (set.removeLast()) {
//                    }
                }
            }
        });

        Button stop = view.findViewById(R.id.stop_btn);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(no_action_flag)
                    Toast.makeText(service.getApplicationContext(), "What are you trying to stop?", Toast.LENGTH_SHORT).show();
                else if(stopped){
                    Toast.makeText(service.getApplicationContext(), "Already stopped", Toast.LENGTH_SHORT).show();
                }
                else {
                    start_flag = false;
                    stopped = true;

                    if (! Python.isStarted()){
                        Python.start(new AndroidPlatform(getContext()));
                    }
                    Python py = Python.getInstance();
                    PyObject pyobj = py.getModule("data_analysis");

                    PyObject obj= pyobj.callAttr("reset");
                    TextView num_of_steps_predicted = (TextView) view.findViewById(R.id.num_of_steps_predicted);
                    num_of_steps_predicted.setText("number of steps : 0");
                }
            }
        });

        Button save = view.findViewById(R.id.save_btn);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(no_action_flag) {
                    Toast.makeText(service.getApplicationContext(), "No data to save yet", Toast.LENGTH_SHORT).show();
                }
                else if(!stopped) {
                    Toast.makeText(service.getApplicationContext(), "Recording must be stopped before saving", Toast.LENGTH_SHORT).show();
                }
                else {
                    start_flag = false;
                    no_action_flag = true;
                    stopped = false;

                    File file = new File("/sdcard/csv_dir/");
                    file.mkdirs();
                    String csv = "/sdcard/csv_dir/" + csvName + ".csv";
                    CSVWriter csvWriter = null;

                    row5 = new String[]{"ESTIMATED NUMBER OF STEPS:", estimatedSteps};
                    try {
                        csvWriter = new CSVWriter(new FileWriter(csv, true));
                        csvWriter.writeNext(row1);
                        csvWriter.writeNext(row2);
                        csvWriter.writeNext(row3);
                        csvWriter.writeNext(row4);
                        csvWriter.writeNext(row5);
                        csvWriter.writeNext(row6);

                        for (String[] row: csv_data) {
                            csvWriter.writeNext(row);
                        }

                        csvWriter.close();
                        chartIndex = 0;

                        LineData data = mpLineChart.getData();
                        ILineDataSet set = data.getDataSetByIndex(0);
                        data.getDataSetByIndex(0);
                        while (set.removeLast()) {
                        }
//                        set = data.getDataSetByIndex(1);
//                        while (set.removeLast()) {
//                        }
//                        set = data.getDataSetByIndex(2);
//                        while (set.removeLast()) {
//                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mpLineChart = (LineChart) view.findViewById(R.id.line_chart);
        lineDataSet1 =  new LineDataSet(emptyDataValues(), "Acceleration");
//        lineDataSet2 =  new LineDataSet(emptyDataValues(), "y-axis ACC");
//        lineDataSet3 =  new LineDataSet(emptyDataValues(), "z-axis ACC");

        lineDataSet1.setColor(Color.BLUE);
//        lineDataSet2.setColor(Color.RED);
//        lineDataSet2.setColor(Color.GREEN);

        dataSets.add(lineDataSet1);
//        dataSets.add(lineDataSet2);
//        dataSets.add(lineDataSet3);

        data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();

        Button buttonClear = (Button) view.findViewById(R.id.button1);
        Button buttonCsvShow = (Button) view.findViewById(R.id.button2);




        buttonClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getContext(),"Clear",Toast.LENGTH_SHORT).show();
                LineData data = mpLineChart.getData();
                ILineDataSet set = data.getDataSetByIndex(0);
                data.getDataSetByIndex(0);
                while(set.removeLast()){}
//                set = data.getDataSetByIndex(1);
//                while(set.removeLast()){}
//                set = data.getDataSetByIndex(2);
//                while(set.removeLast()){}
            }
        });

        buttonCsvShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenLoadCSV();

            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private String[] clean_str(String[] stringsArr){
         for (int i = 0; i < stringsArr.length; i++)  {
             stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }


        return stringsArr;
    }
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] message) {
        if(start_flag) {
            if (hexEnabled) {
                receiveText.append(TextUtil.toHexString(message) + '\n');
            } else {
                String msg = new String(message);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    String msg_to_save = msg;
                    msg_to_save = msg.replace(TextUtil.newline_crlf, TextUtil.emptyString);
                    // check message length
                    if (msg_to_save.length() > 1) {
                        // split message string by ',' char
                        String[] parts = msg_to_save.split(",");
                        // function to trim blank spaces
                        parts = clean_str(parts);
                        float N = (float) Math.sqrt(Math.pow(Float.parseFloat(parts[0]), 2) + Math.pow(Float.parseFloat(parts[1]), 2) + Math.pow(Float.parseFloat(parts[2]), 2));


                        // parse string values, in this case [0] is tmp & [1] is count (t)
                        String row[];
                        if (firstReceive){
                            firstReceive = false;
                            row = new String[]{"0", parts[0], parts[1], parts[2]};
                            startTime = (float) (Float.parseFloat(parts[3])/1000.0);
                        }
                        else{
                            row = new String[]{String.valueOf((Float.parseFloat(parts[3])/1000.0-startTime)), parts[0], parts[1], parts[2]};
                        }
                        csv_data.add(row);

                        // add received values to line dataset for plotting the linechart
                        data.addEntry(new Entry(chartIndex, N), 0);
                        lineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
//                        data.addEntry(new Entry(chartIndex, Float.parseFloat(parts[1])), 1);
//                        lineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
//                        data.addEntry(new Entry(chartIndex, Float.parseFloat(parts[2])), 2);
//                        lineDataSet1.notifyDataSetChanged(); // let the data know a dataSet changed
//                        lineDataSet2.notifyDataSetChanged(); // let the data know a dataSet changed
//                        lineDataSet3.notifyDataSetChanged(); // let the data know a dataSet changed
                        mpLineChart.notifyDataSetChanged(); // let the chart know it's data changed
                        mpLineChart.invalidate(); // refresh
                        chartIndex++;


                        TextView num_of_steps_predicted = (TextView) view.findViewById(R.id.num_of_steps_predicted);



                        if (! Python.isStarted()){
                            Python.start(new AndroidPlatform(getContext()));
                        }
                        Python py = Python.getInstance();
                        PyObject pyobj = py.getModule("data_analysis");
                        PyObject obj= pyobj.callAttr("step_count", N);
                        num_of_steps_predicted.setText("number of steps : " + obj.toString());
                        estimatedSteps = obj.toString();

                    }

                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // send msg to function that saves it to csv
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        Editable edt = receiveText.getEditableText();
                        if (edt != null && edt.length() > 1)
                            edt.replace(edt.length() - 2, edt.length(), "");
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
        receive(data);}
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private ArrayList<Entry> emptyDataValues()
    {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        return dataVals;
    }

    private void OpenLoadCSV(){
        Intent intent = new Intent(getContext(),LoadCSV.class);
        startActivity(intent);
    }

}
