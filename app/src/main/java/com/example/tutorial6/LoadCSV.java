package com.example.tutorial6;


import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import java.util.List;



public class LoadCSV extends AppCompatActivity {
    private static String EXTRA_INSTANCE_ID;
    private static Build.VERSION Util;
    String selectedFile;
    LineChart lineChart;
    ArrayList<String[]> csvData;
    String num_of_predicted_steps_string;


//    @Nullable
//    private static PendingIntent createBroadcastIntent(
//            String action, Context context, int instanceId) {
//        Intent intent = new Intent(action).setPackage(context.getPackageName());
//        intent.putExtra(EXTRA_INSTANCE_ID, instanceId);
//
//        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
//            } else {
//                pendingFlags |= PendingIntent.FLAG_MUTABLE;
//            }
//        }
//
//        return PendingIntent.getBroadcast(context, instanceId, intent, pendingFlags);
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_csv);
        Button BackButton = findViewById(R.id.button_back);
        Button LoadButton = findViewById(R.id.button_load);
        TextView num_of_steps_predicted_button= findViewById(R.id.num_of_steps_predicted_in_load);
        lineChart = findViewById(R.id.line_chart);

        csvData = new ArrayList<>();
//        PendingIntent pendingIntent;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            pendingIntent = PendingIntent.getActivity(this,
//                    0, new Intent(this, getClass()).addFlags(
//                            Intent.FLAG_ACTIVITY_SINGLE_TOP),
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
//        } else {
//            pendingIntent = PendingIntent.getActivity(this,
//                    0, new Intent(this, getClass()).addFlags(
//                            Intent.FLAG_ACTIVITY_SINGLE_TOP),
//                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//        }

        String directoryPath = "/sdcard/csv_dir/";
        List<String> filenames = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filenames.add(file.getName());
                }
            }
        }

        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filenames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFile = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case when no item is selected
            }
        });

        LoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                csvData = CsvRead("/sdcard/csv_dir/" + selectedFile);
                num_of_predicted_steps_string = csvData.get(4)[0] + csvData.get(4)[1];
                num_of_steps_predicted_button.setText(num_of_predicted_steps_string);
                updateGraph();

            }
        });

        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClickBack();
            }
        });
    }

    private void ClickBack() {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        lineChart.clear(); // Clear the line chart to release resources
    }

    private ArrayList<String[]> CsvRead(String path) {
        ArrayList<String[]> CsvData = new ArrayList<>();

        try {
            File file = new File(path);
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextline;
            while ((nextline = reader.readNext()) != null) {
                if (nextline != null) {
                    CsvData.add(nextline);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CsvData;
    }

        private ArrayList<Entry> DataValues(ArrayList<String[]> csvData) {

            ArrayList<Entry> dataVals = new ArrayList<>();
            for (int i = 7; i < csvData.size(); i++) {
                dataVals.add(new Entry(i-7, ((float) Math.sqrt(Math.pow(Float.parseFloat(csvData.get(i)[1]), 2) + Math.pow(Float.parseFloat(csvData.get(i)[2]), 2) + Math.pow(Float.parseFloat(csvData.get(i)[3]), 2)))));
            }
            return dataVals;
        }

        private void updateGraph() {

            LineDataSet lineDataSet1 = new LineDataSet(DataValues(csvData), "Acceleration");
            lineDataSet1.setColor(Color.BLUE);
            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(lineDataSet1);
            LineData data = new LineData(dataSets);
            lineChart.setData(data);
            lineChart.invalidate();

    }
}
