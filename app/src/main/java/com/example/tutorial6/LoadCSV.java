package com.example.tutorial6;


import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

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

//public class LoadCSV extends AppCompatActivity {
//    String selectedFile;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_load_csv);
//        Button BackButton = (Button) findViewById(R.id.button_back);
//        Button LoadButton = (Button) findViewById(R.id.button_load);
//        LineChart lineChart = (LineChart) findViewById(R.id.line_chart);
//
//        ArrayList<String[]> csvData = new ArrayList<>();
//
//        String directoryPath = "/sdcard/csv_dir/";
//        List<String> filenames = new ArrayList<String>();
//        File directory = new File(directoryPath);
//        File[] files = directory.listFiles();
//
//
//        if (files != null) {
//            for (File file : files) {
//                if (file.isFile()) {
//                    filenames.add(file.getName());
//
//                }
//            }
//        }
//        selectedFile = filenames.get(0);
//
//        Spinner spinner = (Spinner) findViewById(R.id.spinner);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filenames);
//        // Set the layout for the dropdown menu
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        // Set the adapter to the spinner
//        spinner.setAdapter(adapter);
//
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // Get the selected item from the spinner
//                selectedFile = parent.getItemAtPosition(position).toString();
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Handle the case when no item is selected
//                // This method is optional and can be left empty if not needed
//            }
//        });
//
//
//
//        csvData= CsvRead("/sdcard/csv_dir/"+selectedFile);
//        LineDataSet lineDataSet1 =  new LineDataSet(DataValues(csvData, 0),"x-axis ACC");
//        LineDataSet lineDataSet2 =  new LineDataSet(DataValues(csvData, 1),"y-axis ACC");
//        LineDataSet lineDataSet3 =  new LineDataSet(DataValues(csvData, 2),"z-axis ACC");
//
//        lineDataSet1.setColor(Color.BLUE);
//        lineDataSet2.setColor(Color.RED);
//        lineDataSet3.setColor(Color.GREEN);
//
//        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
//        dataSets.add(lineDataSet1);
//        dataSets.add(lineDataSet2);
//        dataSets.add(lineDataSet3);
//        LineData data = new LineData(dataSets);
//        lineChart.setData(data);
//        lineChart.invalidate();
//
//
//
//
//        BackButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ClickBack();
//            }
//        });
//    }
//
//    private void ClickBack(){
//        finish();
//
//    }
//
//    private ArrayList<String[]> CsvRead(String path){
//        ArrayList<String[]> CsvData = new ArrayList<>();
//        try {
//            File file = new File(path);
//            CSVReader reader = new CSVReader(new FileReader(file));
//            String[]nextline;
//            while((nextline = reader.readNext())!= null){
//                if(nextline != null){
//                    CsvData.add(nextline);
//
//                }
//            }
//
//        }catch (Exception e){}
//        return CsvData;
//    }
//
//    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData, int index){
//        ArrayList<Entry> dataVals = new ArrayList<Entry>();
//        for (int i = 0; i < csvData.size(); i++){
//
//            dataVals.add(new Entry(Integer.parseInt(csvData.get(i)[3]),
//                    Float.parseFloat(csvData.get(i)[index])));
//
//
//        }
//
//        return dataVals;
//    }
//
//}

public class LoadCSV extends AppCompatActivity {
    String selectedFile;
    LineChart lineChart;
    ArrayList<String[]> csvData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_csv);
        Button BackButton = findViewById(R.id.button_back);
        Button LoadButton = findViewById(R.id.button_load);
        lineChart = findViewById(R.id.line_chart);

        csvData = new ArrayList<>();

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

    private ArrayList<Entry> DataValues(ArrayList<String[]> csvData, int index) {
        ArrayList<Entry> dataVals = new ArrayList<>();
        for (int i = 6; i < csvData.size(); i++) {
            dataVals.add(new Entry(i-6, Float.parseFloat(csvData.get(i)[index])));
        }
        return dataVals;
    }

    private void updateGraph() {
        LineDataSet lineDataSet1 = new LineDataSet(DataValues(csvData, 1), "x-axis ACC");
        LineDataSet lineDataSet2 = new LineDataSet(DataValues(csvData, 2), "y-axis ACC");
        LineDataSet lineDataSet3 = new LineDataSet(DataValues(csvData, 3), "z-axis ACC");

        lineDataSet1.setColor(Color.BLUE);
        lineDataSet2.setColor(Color.RED);
        lineDataSet3.setColor(Color.GREEN);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet1);
        dataSets.add(lineDataSet2);
        dataSets.add(lineDataSet3);
        LineData data = new LineData(dataSets);
        lineChart.setData(data);
        lineChart.invalidate();
    }
}
