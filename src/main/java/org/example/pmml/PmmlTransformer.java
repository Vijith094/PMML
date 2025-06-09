package org.example.pmml;

import jakarta.xml.bind.JAXBException;
import org.example.config.PreprocessingConfig;
import org.example.config.PreprocessingStep;
import org.dmg.pmml.*;
import org.jpmml.model.PMMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;


public class PmmlTransformer {
    private static final Logger logger = LoggerFactory.getLogger(PmmlTransformer.class);

    /**
     * Loads a PMML model from a file path. If the path is null or the file doesn't exist,
     * creates a new, empty PMML object.
     *
     * @param inputPmmlPath Path to the input PMML file. Can be null.
     * @return A PMML object.
     * @throws IOException If there's an error reading the file.
     * @throws JAXBException If there's an error parsing the PMML XML.
     */
    public PMML loadOrCreatePmml(String inputPmmlPath) throws Exception {
        if (inputPmmlPath != null && !inputPmmlPath.isEmpty()) {
            File pmmlFile = new File(inputPmmlPath);
            if (pmmlFile.exists() && pmmlFile.isFile()) {
                logger.info("Loading PMML from: {}", inputPmmlPath);
                try (InputStream is = new FileInputStream(pmmlFile)) {
                    return PMMLUtil.unmarshal(is);
                }
            } else {
                logger.warn("Input PMML file not found: {}. Creating a new PMML object.", inputPmmlPath);
            }
        } else {
            logger.info("No input PMML specified. Creating a new PMML object.");
        }
        PMML pmml = new PMML();
        pmml.setHeader(new Header().setDescription("PMML document with preprocessing transformations"));
        pmml.setDataDictionary(new DataDictionary());
        return pmml;
    }

    /**
     * Applies preprocessing transformations to the PMML model based on the configuration.
     *
     * @param pmml The PMML model to transform.
     * @param config The preprocessing configuration.
     * @param defaultDataType Default DataType for original fields if not in DataDictionary.
     * @param defaultOpType Default OpType for original fields if not in DataDictionary.
     */
    public void applyTransformations(PMML pmml, PreprocessingConfig config, String defaultDataType, String defaultOpType) {
        if (pmml.getDataDictionary() == null) {
            pmml.setDataDictionary(new DataDictionary());
        }
        DataDictionary dataDictionary = pmml.getDataDictionary();

        if (pmml.getTransformationDictionary() == null) {
            logger.warn("PMML has LocalTransformations but no global TransformationDictionary. Adding a new global TransformationDictionary.");
            pmml.setTransformationDictionary(new TransformationDictionary());
        } else if (pmml.getTransformationDictionary() == null) {
            pmml.setTransformationDictionary(new TransformationDictionary());
        }
        TransformationDictionary transformationDictionary = pmml.getTransformationDictionary();

        for (PreprocessingStep step : config.getPreprocessingSteps()) {
            logger.info("Processing step for variable: {} with transformation: {}", step.getVariableName(), step.getTransformation());
            String originalFieldName = step.getVariableName();

            Optional<DataField> existingOriginalField = dataDictionary.getDataFields().stream()
                    .filter(df -> df.getName().equals(originalFieldName))
                    .findFirst();

            if (!existingOriginalField.isPresent()) {
//                DataType dt = defaultDataType != null ? DataType.fromValue(defaultDataType) : DataType.INTEGER;
//                OpType ot = defaultOpType != null ? OpType.fromValue(defaultOpType) : OpType.CATEGORICAL;
                DataType dt = DataType.INTEGER;
                OpType ot =  OpType.CATEGORICAL;

                if (step.getParams().containsKey("originalDataType")) {
                    dt = DataType.fromValue((String)step.getParams().get("originalDataType"));
                }
                if (step.getParams().containsKey("originalOpType")) {
                    ot = OpType.fromValue((String)step.getParams().get("originalOpType"));
                }

                logger.warn("Original field '{}' not found in DataDictionary. Adding with DataType: {}, OpType: {}.",
                        originalFieldName, dt.value(), ot.value());
                dataDictionary.addDataFields(TransformationFactory.createDataField(originalFieldName, ot, dt));
            }


            List<DerivedField> derivedFields = TransformationFactory.createDerivedFields(step, pmml);
            for (DerivedField derivedField : derivedFields) {
                transformationDictionary.getDerivedFields().removeIf(df -> df.getName().equals(derivedField.getName()));
                transformationDictionary.addDerivedFields(derivedField);
                logger.info("Added/Updated DerivedField: {} to TransformationDictionary.", derivedField.getName());

                dataDictionary.getDataFields().removeIf(df -> df.getName().equals(derivedField.getName()));
                dataDictionary.addDataFields(TransformationFactory.createDataField(
                        derivedField.getName(),
                        derivedField.getOpType(),
                        derivedField.getDataType()
                ));
                logger.info("Added/Updated DataField: {} to DataDictionary.", derivedField.getName());
            }
        }
        dataDictionary.setNumberOfFields(dataDictionary.getDataFields().size());
    }

    /**
     * Saves the PMML model to an output file.
     *
     * @param pmml The PMML model to save.
     * @param outputPath The path to the output PMML file.
     * @throws IOException If there's an error writing the file.
     * @throws JAXBException If there's an error marshalling the PMML object.
     */
    public void savePmml(PMML pmml, String outputPath) throws IOException, JAXBException {
        logger.info("Saving PMML to: {}", outputPath);
        File outputFile = new File(outputPath);
        try (OutputStream os = new FileOutputStream(outputFile)) {
            PMMLUtil.marshal(pmml, os);
        }
        logger.info("PMML saved successfully.");
    }
}
