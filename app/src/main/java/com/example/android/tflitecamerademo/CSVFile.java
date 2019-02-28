package com.example.android.tflitecamerademo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVFile {
    String fileName;

    public CSVFile(String fileName){
        this.fileName = fileName;
    }

    public List read() throws FileNotFoundException {
        List resultList = new ArrayList();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                resultList.add(row);
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: "+ex);
        }
        return resultList;
    }
}
