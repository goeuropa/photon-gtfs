/* (C)2025 */
package de.komoot.photon.gtfs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public abstract class CsvBaseReader<T> {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CsvBaseReader.class);

    private final String fileName;

    protected List<T> gtfsObjects;

    protected CsvBaseReader(String dirName, String fileName) {
        this.fileName = dirName + fileName;
    }

    protected abstract T handleRecord(CSVRecord record)
            throws ParseException, NumberFormatException;

    private void parse() {
        CSVRecord record = null;
        try {
            LOGGER.debug("Parsing CSV file {} ...", fileName);

            Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));

            // Deal with the possible BOM character at the beginning of the file
            in.mark(1);
            int firstRead = in.read();
            final int BOM_CHARACTER = 0xFEFF;
            if (firstRead != BOM_CHARACTER) in.reset();

            CSVFormat formatter = CSVFormat.Builder
                    .create()
                    .setHeader()
                    .setCommentMarker('-')
                    .build();

            // Parse the file
            Iterable<CSVRecord> records = formatter.parse(in);

            LOGGER.debug("Finished CSV parsing of file {}.", fileName);

            for (CSVRecord strings : records) {
                record = strings;

                if (record.size() == 0) continue;

                T gtfsObject;
                try {
                    gtfsObject = handleRecord(record);
                } catch (ParseException e) {
                    LOGGER.error(
                            "ParseException occurred for record {} "
                                    + "(comment lines not included when determing record #) for "
                                    + "filename {} . {}",
                            record.getRecordNumber(),
                            fileName,
                            e.getMessage());

                    continue;
                } catch (NumberFormatException e) {
                    LOGGER.error(
                            "NumberFormatException occurred for record {} "
                                    + "(comment lines not included when determing record #) "
                                    + "for filename {} . {}",
                            record.getRecordNumber(),
                            fileName,
                            e.getMessage());
                    continue;
                }

                if (gtfsObject != null) gtfsObjects.add(gtfsObject);
            }
            in.close();
            long numberRecords = 0;
            if (record != null) numberRecords = record.getRecordNumber();

            LOGGER.info(
                    "Finished parsing {} records from file {} .",
                    numberRecords,
                    fileName
            );

        } catch (FileNotFoundException e) {
            LOGGER.info("CSV file {} not found but OK because this file " + "not required.", fileName);
        } catch (IOException e) {
            LOGGER.error("IOException occurred when reading in filename {}.", fileName, e);
        }
    }

    public List<T> get() {
        return get(1500);
    }

    public List<T> get(int initialSize) {
        gtfsObjects = new ArrayList<>(initialSize);
        parse();

        return gtfsObjects;
    }
}
