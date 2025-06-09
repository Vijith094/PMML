package org.example;

import org.example.config.PreprocessingConfig;
import org.example.pmml.PmmlTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.cli.*;
import org.dmg.pmml.PMML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class PmmlPreprocessorCli {

    private static final Logger logger = LoggerFactory.getLogger(PmmlPreprocessorCli.class);
    private static final String OPT_INPUT_PMML = "input";
    private static final String OPT_CONFIG_JSON = "config";
    private static final String OPT_OUTPUT_PMML = "output";
    private static final String OPT_DEFAULT_DATATYPE = "defaultDataType";
    private static final String OPT_DEFAULT_OPTYPE = "defaultOpType";
    private static final String OPT_HELP = "help";


    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("i", OPT_INPUT_PMML, true, "Path to the input PMML file (optional)");
        options.addRequiredOption("c", OPT_CONFIG_JSON, true, "Path to the JSON preprocessing configuration file");
        options.addRequiredOption("o", OPT_OUTPUT_PMML, true, "Path for the output PMML file");
        options.addOption("ddt", OPT_DEFAULT_DATATYPE, true, "Default DataType for original fields if not in DataDictionary (e.g., STRING, DOUBLE)");
        options.addOption("dot", OPT_DEFAULT_OPTYPE, true, "Default OpType for original fields if not in DataDictionary (e.g., CATEGORICAL, CONTINUOUS)");
        options.addOption("h", OPT_HELP, false, "Show help");


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error("Error parsing command line arguments: {}", e.getMessage());
            formatter.printHelp("PmmlPreprocessorCli", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption(OPT_HELP)) {
            formatter.printHelp("PmmlPreprocessorCli", options);
            System.exit(0);
        }

        String inputPmmlPath = cmd.getOptionValue(OPT_INPUT_PMML);
        String configJsonPath = cmd.getOptionValue(OPT_CONFIG_JSON);
        String outputPmmlPath = cmd.getOptionValue(OPT_OUTPUT_PMML);
        String defaultDataType = cmd.getOptionValue(OPT_DEFAULT_DATATYPE, "STRING"); // Default to STRING
        String defaultOpType = cmd.getOptionValue(OPT_DEFAULT_OPTYPE, "CATEGORICAL"); // Default to CATEGORICAL


        try {
            // 1. Load Preprocessing Configuration
            ObjectMapper jsonMapper = new ObjectMapper();
            PreprocessingConfig preprocessingConfig = jsonMapper.readValue(new File(configJsonPath), PreprocessingConfig.class);
            logger.info("Successfully loaded preprocessing configuration from: {}", configJsonPath);

            // 2. Initialize PmmlTransformer
            PmmlTransformer transformer = new PmmlTransformer();

            // 3. Load or Create PMML
            PMML pmml = transformer.loadOrCreatePmml(inputPmmlPath);

            // 4. Apply Transformations
            // Use global defaults from config if present, otherwise use CLI/hardcoded defaults
            String actualDefaultDataType = preprocessingConfig.getDefaultDataTypeForOriginals() != null ?
                    preprocessingConfig.getDefaultDataTypeForOriginals() : defaultDataType;
            String actualDefaultOpType = preprocessingConfig.getDefaultOpTypeForOriginals() != null ?
                    preprocessingConfig.getDefaultOpTypeForOriginals() : defaultOpType;

            transformer.applyTransformations(pmml, preprocessingConfig, actualDefaultDataType, actualDefaultOpType);


            // 5. Save PMML
            transformer.savePmml(pmml, outputPmmlPath);

            logger.info("PMML preprocessing completed successfully. Output at: {}", outputPmmlPath);

        } catch (IOException e) {
            logger.error("File I/O error: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("An unexpected error occurred: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}