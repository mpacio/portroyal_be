package com.matteopaciolla.portroyal.facades;

import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.googlecode.jcsv.writer.CSVWriter;
import com.googlecode.jcsv.writer.internal.CSVWriterBuilder;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.exceptions.IOGameException;
import com.matteopaciolla.portroyal.util.io.MoveRecordConverter;
import com.matteopaciolla.portroyal.util.io.MoveRecordParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class IOFacade {

    public static final String[] headers = new String[] {"timeIndex", "runningPlayerIndex", "move", "choiceIndex", "pickPlayerIndex", "expeditionEmployees", "notes"};
    public static final char CSV_SEPARATOR = ';';
    public static final char CSV_QUOTE = '"';
    public static final char CSV_COMMENT = '#';
    public static final String CSV_NEW_LINE = "\r\n";
    public static final CSVStrategy INPUT_STRATEGY = new CSVStrategy(CSV_SEPARATOR, CSV_QUOTE, CSV_COMMENT, true, true);
    public static final CSVStrategy OUTPUT_STRATEGY = new CSVStrategy(CSV_SEPARATOR, CSV_QUOTE, CSV_COMMENT, false, true);
    public static final String CSV_FILES_FOLDER = "csv_files/";

    public static void saveMovesToCSV(List<MoveRecord> moves, String filename) throws IOGameException {
        try {
            java.io.File dir = new java.io.File(CSV_FILES_FOLDER);
            if (!dir.exists()) {
                var res = dir.mkdirs();
            }
            try (FileWriter fileWriter = new FileWriter(CSV_FILES_FOLDER + filename + ".csv")) {
                try (CSVWriter<MoveRecord> csvWriter = new CSVWriterBuilder<MoveRecord>(fileWriter)
                        .strategy(OUTPUT_STRATEGY)
                        .entryConverter(new MoveRecordConverter()).build()) {
                    fileWriter.write(String.join(String.valueOf(CSV_SEPARATOR), headers) + CSV_NEW_LINE);
                    csvWriter.writeAll(moves);
                }
            }
        } catch (IOException e) {
            throw new IOGameException(e);
        }
    }

    public static List<MoveRecord> loadMovesFromResourceCSV(URL resource) throws IOGameException {
        assert resource != null;
        try (FileReader reader = new FileReader(resource.getFile())) {
            return loadMovesFromCSV(reader);
        } catch (Exception e) {
            throw new IOGameException(e);
        }
    }

    private static List<MoveRecord> loadMovesFromCSV(FileReader reader) throws IOGameException {
        try (CSVReader<MoveRecord> csvReader = new CSVReaderBuilder<MoveRecord>(reader)
                .strategy(INPUT_STRATEGY)
                .entryParser(new MoveRecordParser()).build()) {
            return csvReader.readAll();
        } catch (IOException e) {
            throw new IOGameException(e);
        }
    }

    public static List<MoveRecord> loadMovesFromResourceCSV(String filename) throws IOGameException {
        URL resource = IOFacade.class.getClassLoader().getResource(filename + ".csv");
        return loadMovesFromResourceCSV(resource);
    }

    public static List<MoveRecord> loadMovesFromExternalCSV(String filename) throws IOGameException {
        try (FileReader reader = new FileReader(CSV_FILES_FOLDER + filename + ".csv")) {
            return loadMovesFromCSV(reader);
        } catch (Exception e) {
            throw new IOGameException(e);
        }
    }
}
