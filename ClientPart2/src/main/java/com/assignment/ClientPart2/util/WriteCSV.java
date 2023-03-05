package com.assignment.ClientPart2.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteCSV {
    private final String filePath;
    private final BufferedWriter writer;

    public WriteCSV(String filePath) throws IOException {
        this.filePath = filePath;
        this.writer = new BufferedWriter(new FileWriter(filePath, true));
    }

    public synchronized void write(String[] data) throws IOException {
        for (String value : data) {
            writer.write(value);
            writer.write(",");
        }
        writer.newLine();
        writer.flush();
    }

    public synchronized void close() throws IOException {
        writer.close();
    }
}


