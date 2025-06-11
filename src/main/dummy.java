import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.FileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.AvroRuntimeException; // Import for Avro specific exceptions

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class AvroToJsonConverter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java AvroToJsonConverter <inputAvroFilePath> <outputJsonFilePath>");
            return;
        }

        String inputAvroFilePath = args[0];
        String outputJsonFilePath = args[1];

        File avroFile = new File(inputAvroFilePath);
        File jsonFile = new File(outputJsonFilePath);

        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
        FileReader<GenericRecord> dataFileReader = null;
        OutputStreamWriter writer = null;

        long successfulRecords = 0;
        long failedRecords = 0;
        long currentRecordNumber = 0; // To keep track of which record we are trying to process

        try {
            // --- Step 1: Open Avro file reader ---
            dataFileReader = DataFileReader.openReader(avroFile, datumReader);
            Schema schema = dataFileReader.getSchema(); // Get the schema from the Avro file

            // --- Step 2: Open JSON output writer ---
            writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);

            // Create a DatumWriter for converting GenericRecord to JSON
            DatumWriter<GenericRecord> jsonDatumWriter = new org.apache.avro.generic.GenericDatumWriter<>(schema);
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, writer);

            GenericRecord record = null;
            // --- Step 3: Iterate and Process Records ---
            while (dataFileReader.hasNext()) {
                currentRecordNumber++; // Increment before attempting to read the record

                try {
                    // Try to read the next record
                    record = dataFileReader.next(record); // Reuse the record object for efficiency

                    // Try to write the record to JSON
                    jsonDatumWriter.write(record, encoder);
                    encoder.flush(); // Ensure the JSON is written immediately
                    writer.write(System.lineSeparator()); // Add a newline after each JSON record
                    successfulRecords++;

                } catch (AvroRuntimeException e) {
                    // Catches issues where the Avro data itself is malformed or doesn't match schema
                    System.err.println("ERROR: Avro data corruption detected at record " + currentRecordNumber +
                                       " in file '" + inputAvroFilePath + "'. Skipping record.");
                    System.err.println("       Details: " + e.getMessage());
                    failedRecords++;
                    // No need to write to JSON for this corrupted record
                } catch (IOException e) {
                    // Catches general I/O errors during reading or writing
                    System.err.println("ERROR: I/O error during processing record " + currentRecordNumber +
                                       " in file '" + inputAvroFilePath + "'. Skipping record.");
                    System.err.println("       Details: " + e.getMessage());
                    failedRecords++;
                    // Depending on the severity of IOException, you might want to break here if it's not recoverable
                    // For now, we'll try to continue.
                }
            }
            System.out.println("--- Conversion Summary ---");
            System.out.println("Successfully converted records: " + successfulRecords);
            System.out.println("Records skipped due to corruption: " + failedRecords);
            System.out.println("Output written to: " + outputJsonFilePath);

        } catch (IOException e) {
            // This catch block handles errors that occur before the loop, like file not found,
            // or during the initial opening of the Avro file/JSON writer.
            System.err.println("FATAL ERROR: Could not process Avro file '" + inputAvroFilePath +
                               "' or initialize output file '" + outputJsonFilePath + "'.");
            System.err.println("       Details: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for fatal errors
        } finally {
            // --- Step 4: Close Resources ---
            try {
                if (dataFileReader != null) {
                    dataFileReader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                System.err.println("ERROR: Error closing resources: " + e.getMessage());
            }
        }
    }
}
