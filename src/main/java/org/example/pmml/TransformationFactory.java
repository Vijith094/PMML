package org.example.pmml;

import org.example.config.DiscretizeBinDef;
import org.example.config.MapValueItemDef; // Added import
import org.example.config.PreprocessingStep;
import org.example.config.TransformationType;
import com.fasterxml.jackson.core.type.TypeReference; // Added import
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dmg.pmml.*;
import org.jpmml.model.PMMLUtil; // For JAXB marshalling/unmarshalling if saving/loading manually

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransformationFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates a DataField. Helper method.
     *
     * @param name Name of the field.
     * @param opType Operational type (e.g., CONTINUOUS, CATEGORICAL).
     * @param dataType Data type (e.g., DOUBLE, STRING).
     * @return A new DataField instance.
     */
    public static DataField createDataField(String name, OpType opType, DataType dataType) {
        DataField dataField = new DataField(name, opType, dataType);
        return dataField;
    }

    /**
     * Creates one or more DerivedField PMML elements based on the preprocessing step configuration.
     *
     * @param step The preprocessing step configuration.
     * @param pmml The PMML model, used to potentially infer data types of original fields.
     * @return A list of DerivedField elements. Empty if transformation is not supported or invalid.
     */
    public static List<DerivedField> createDerivedFields(PreprocessingStep step, PMML pmml) {
        List<DerivedField> derivedFields = new ArrayList<>();
        String originalFieldName = step.getVariableName();
        Map<String, Object> params = step.getParams();

        String outputName;
        DataType derivedDataType;
        OpType derivedOpType;

        switch (step.getTransformation()) {
            case MIN_MAX_SCALER:
                outputName = (String) params.get("outputName");
                double minOrigin = ((Number) params.get("minOrigin")).doubleValue();
                double maxOrigin = ((Number) params.get("maxOrigin")).doubleValue();
                double minTarget = params.containsKey("minTarget") ? ((Number) params.get("minTarget")).doubleValue() : 0.0;
                double maxTarget = params.containsKey("maxTarget") ? ((Number) params.get("maxTarget")).doubleValue() : 1.0;
                derivedDataType = DataType.fromValue((String) params.getOrDefault("dataType", "DOUBLE"));
                derivedOpType = OpType.fromValue((String) params.getOrDefault("opType", "CONTINUOUS"));


                NormContinuous normContinuous = new NormContinuous();
                        normContinuous.setField(originalFieldName);
                normContinuous.addLinearNorms(
                        new LinearNorm(minOrigin, minTarget),
                        new LinearNorm(maxOrigin, maxTarget)
                );
                DerivedField scaledField = new DerivedField(outputName, derivedOpType, derivedDataType,normContinuous);
                scaledField.setExpression(normContinuous);
                derivedFields.add(scaledField);
                break;

            case LOGARITHMIC: // Natural Log
                outputName = (String) params.get("outputName");
                derivedDataType = DataType.fromValue((String) params.getOrDefault("dataType", "DOUBLE"));
                derivedOpType = OpType.fromValue((String) params.getOrDefault("opType", "CONTINUOUS"));

                Apply logApply = new Apply("ln"); // PMML function for natural log
                logApply.addExpressions(new FieldRef(originalFieldName));
                DerivedField logField = new DerivedField(outputName, derivedOpType, derivedDataType, logApply);
                derivedFields.add(logField);
                break;

            case ONE_HOT_ENCODER:
                List<Object> categories = (List<Object>) params.get("categories");
                String outputPrefix = (String) params.get("outputPrefix");
                derivedDataType = DataType.fromValue((String) params.getOrDefault("dataType", "INTEGER")); // Typically 0 or 1
                derivedOpType = OpType.fromValue((String) params.getOrDefault("opType", "CONTINUOUS")); // Often treated as continuous (0/1)

                for (Object category : categories) {
                    String catStr = String.valueOf(category);
                    String derivedCatFieldName = outputPrefix + "_" + sanitizeName(catStr);


                    Apply ifApply = new Apply("if");
                    Apply equalsApply = new Apply("equal");
                    equalsApply.addExpressions(new FieldRef(originalFieldName), new Constant(catStr));
                    ifApply.addExpressions(equalsApply, new Constant(1), new Constant(0));
                    DerivedField oheField = new DerivedField(derivedCatFieldName, derivedOpType, derivedDataType, ifApply);
                    oheField.setExpression(ifApply);
                    derivedFields.add(oheField);
                }
                break;

//            case DISCRETIZE:
//                outputName = (String) params.get("outputName");
//                List<Map<String, Object>> binDefsMaps = (List<Map<String, Object>>) params.get("bins");
//                derivedDataType = DataType.fromValue((String) params.getOrDefault("dataType", "STRING"));
//                derivedOpType = OpType.fromValue((String) params.getOrDefault("opType", "CATEGORICAL"));
//
//
//                Discretize discretize = new Discretize(originalFieldName);
//
//                for (Map<String, Object> binDefMap : binDefsMaps) {
//                    DiscretizeBinDef binDef = MAPPER.convertValue(binDefMap, DiscretizeBinDef.class);
//                    DiscretizeBin pmmlBin = new DiscretizeBin(binDef.getLabel());
//                    Interval interval = new Interval(Interval.Closure.fromValue(binDef.getClosure()));
//                    if (binDef.getLeftMargin() != null) {
//                        interval.setLeftMargin(binDef.getLeftMargin());
//                    }
//                    if (binDef.getRightMargin() != null) {
//                        interval.setRightMargin(binDef.getRightMargin());
//                    }
//                    pmmlBin.setInterval(interval);
//                    discretize.addDiscretizeBins(pmmlBin);
//                }
//                DerivedField discretizedField = new DerivedField(outputName, derivedOpType, derivedDataType,discretize);
//                derivedFields.add(discretizedField);
//                break;

            case CUSTOM_APPLY:
                String customOutputName = (String) params.get("outputName");
                String pmmlFunction = (String) params.get("pmmlFunction");
                DataType customDataType = DataType.fromValue((String) params.get("dataType"));
                OpType customOpType = OpType.fromValue((String) params.get("opType"));
                List<Map<String, Object>> constantsList = (List<Map<String, Object>>) params.get("constants");
                List<String> argumentFieldNames = (List<String>) params.get("argumentFields");


                Apply customApplyExpr = new Apply(pmmlFunction);

                if (argumentFieldNames != null && !argumentFieldNames.isEmpty()) {
                    for (String argFieldName : argumentFieldNames) {
                        customApplyExpr.addExpressions(new FieldRef(argFieldName));
                    }
                } else {
                    customApplyExpr.addExpressions(new FieldRef(originalFieldName));
                }

                if (constantsList != null) {
                    for (Map<String, Object> constMap : constantsList) {
                        Constant constant = new Constant(String.valueOf(constMap.get("value")));
                        // DataType for Constant can be set if needed, but JPMML often infers
                        customApplyExpr.addExpressions(constant);
                    }
                }
                DerivedField customField = new DerivedField(customOutputName, customOpType, customDataType,customApplyExpr);
                derivedFields.add(customField);
                break;

            case MAP_VALUES:
                outputName = (String) params.get("outputName");
                //derivedDataType = DataType.fromValue((String) params.get("dataType"));
                //derivedOpType = OpType.fromValue((String) params.get("opType"));
                derivedDataType = DataType.INTEGER;
                derivedOpType = OpType.CATEGORICAL;

                List<MapValueItemDef> valueMapItems = MAPPER.convertValue(params.get("valueMap"), new TypeReference<List<MapValueItemDef>>() {});

                Object defaultValue = params.get("defaultValue"); // Can be null
                Object mapMissingTo = params.get("mapMissingTo"); // Can be null

                // The column in the InlineTable that contains the input values to match.
                String inputColumnName = (String) params.getOrDefault("inputColumn", "input");
                // The column in the InlineTable that contains the output values.
                String outputColumnName = (String) params.getOrDefault("outputColumn", "output");



                MapValues mapValues = new MapValues(); // outputColumn refers to the column in InlineTable

                if (defaultValue != null) {
                    mapValues.setDefaultValue(String.valueOf(defaultValue));
                }
                if (mapMissingTo != null) {
                    mapValues.setMapMissingTo(String.valueOf(mapMissingTo));
                }

                // FieldColumnPair links the original field to the 'input' column in the InlineTable
                mapValues.addFieldColumnPairs(new FieldColumnPair(originalFieldName, inputColumnName));

                InlineTable inlineTable = new InlineTable();
                for (MapValueItemDef item : valueMapItems) {
                    Row row = new Row();
                    // The order of cells in the row should match the conceptual columns of the InlineTable.
                    // Here, we assume first cell is 'inputColumnName', second is 'outputColumnName'.
                    // If more complex InlineTable structures are needed, this needs more robust column definition.
                    row.addContent(String.valueOf(item.getInput()));  // Corresponds to 'inputColumnName'
                    row.addContent(String.valueOf(item.getOutput())); // Corresponds to 'outputColumnName'
                    inlineTable.addRows(row);
                }
                mapValues.setInlineTable(inlineTable);
                DerivedField mapValuesField = new DerivedField(outputName, derivedOpType, derivedDataType,mapValues);
                mapValuesField.setExpression(mapValues);
                derivedFields.add(mapValuesField);
                break;

            default:
                System.err.println("Unsupported transformation type: " + step.getTransformation());
                break;
        }
        return derivedFields;
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
