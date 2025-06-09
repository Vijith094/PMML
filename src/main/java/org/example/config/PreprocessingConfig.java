package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PreprocessingConfig {
    @JsonProperty(required = true)
    private List<PreprocessingStep> preprocessingSteps;

    // Optional: Default DataType and OpType for original fields if not found in PMML
    // and not specified per transformation.
    private String defaultDataTypeForOriginals; // e.g., "STRING"
    private String defaultOpTypeForOriginals;   // e.g., "CATEGORICAL"


    public List<PreprocessingStep> getPreprocessingSteps() {
        return preprocessingSteps;
    }

    public void setPreprocessingSteps(List<PreprocessingStep> preprocessingSteps) {
        this.preprocessingSteps = preprocessingSteps;
    }

    public String getDefaultDataTypeForOriginals() {
        return defaultDataTypeForOriginals;
    }

    public void setDefaultDataTypeForOriginals(String defaultDataTypeForOriginals) {
        this.defaultDataTypeForOriginals = defaultDataTypeForOriginals;
    }

    public String getDefaultOpTypeForOriginals() {
        return defaultOpTypeForOriginals;
    }

    public void setDefaultOpTypeForOriginals(String defaultOpTypeForOriginals) {
        this.defaultOpTypeForOriginals = defaultOpTypeForOriginals;
    }
}
