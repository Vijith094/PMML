package org.example.config;

public enum TransformationType {
    MIN_MAX_SCALER,
    LOGARITHMIC, // Natural logarithm
    ONE_HOT_ENCODER,
    DISCRETIZE,
    CUSTOM_APPLY, // For applying standard PMML functions
    MAP_VALUES
}
