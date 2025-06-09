package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CustomApplyDef {
    @JsonProperty(required = true)
    private String outputName; // Name of the derived field

    @JsonProperty(required = true)
    private String pmmlFunction; // Name of the PMML function (e.g., "log10", "uppercase", "if")

    // PMML DataType: STRING, INTEGER, FLOAT, DOUBLE, BOOLEAN, DATE, TIME, DATETIME, etc.
    @JsonProperty(required = true)
    private String dataType;

    // PMML OpType: CONTINUOUS, CATEGORICAL, ORDINAL
    @JsonProperty(required = true)
    private String opType;

    // Optional: For functions that take constants as arguments in addition to the main field.
    // Example: {"value": 10, "dataType": "INTEGER"}
    private List<Map<String, Object>> constants;

    // Optional: For functions that take multiple field arguments.
    // If not provided, 'variableName' from PreprocessingStep is assumed as the primary argument.
    // Example: ["field1", "field2"]
    private List<String> argumentFields;


    public String getOutputName() { return outputName; }
    public void setOutputName(String outputName) { this.outputName = outputName; }
    public String getPmmlFunction() { return pmmlFunction; }
    public void setPmmlFunction(String pmmlFunction) { this.pmmlFunction = pmmlFunction; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getOpType() { return opType; }
    public void setOpType(String opType) { this.opType = opType; }
    public List<Map<String, Object>> getConstants() { return constants; }
    public void setConstants(List<Map<String, Object>> constants) { this.constants = constants; }
    public List<String> getArgumentFields() { return argumentFields; }
    public void setArgumentFields(List<String> argumentFields) { this.argumentFields = argumentFields; }
}
