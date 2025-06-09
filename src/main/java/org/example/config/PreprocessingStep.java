package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PreprocessingStep {
    @JsonProperty(required = true)
    private String variableName; // Name of the original variable to transform

    @JsonProperty(required = true)
    private TransformationType transformation; // Type of transformation

    // Parameters specific to the transformation type
    // Examples:
    // For MIN_MAX_SCALER: {"outputName": "var_scaled", "minOrigin": 0.0, "maxOrigin": 100.0, "minTarget": 0.0, "maxTarget": 1.0, "dataType": "DOUBLE", "opType": "CONTINUOUS"}
    // For LOGARITHMIC: {"outputName": "var_log", "dataType": "DOUBLE", "opType": "CONTINUOUS"}
    // For ONE_HOT_ENCODER: {"categories": ["A", "B", "C"], "outputPrefix": "var_cat", "dataType": "INTEGER", "opType": "CONTINUOUS"} (dataType/opType for the 0/1 output)
    // For DISCRETIZE: {"outputName": "var_binned", "bins": [...], "dataType": "STRING", "opType": "CATEGORICAL"}
    // For CUSTOM_APPLY: (see CustomApplyDef)
    // For MAP_VALUES: {"outputName": "var_mapped", "dataType": "INTEGER", "opType": "CATEGORICAL", "valueMap": [{"input": "A", "output": 1}, ...], "defaultValue": 0, "mapMissingTo": null, "inputColumn": "original_input_col_name", "outputColumn": "target_output_col_name"}
    @JsonProperty(required = true)
    private Map<String, Object> params;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public TransformationType getTransformation() {
        return transformation;
    }

    public void setTransformation(TransformationType transformation) {
        this.transformation = transformation;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
