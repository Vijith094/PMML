package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscretizeBinDef {
    @JsonProperty(required = true)
    private String label; // The value the bin will take (binValue in PMML)

    // At least one of leftMargin or rightMargin must be present
    private Double leftMargin; // null if unbounded on the left
    private Double rightMargin; // null if unbounded on the right

    // PMML Interval closure: openClosed, openOpen, closedOpen, closedClosed
    @JsonProperty(required = true)
    private String closure = "closedClosed"; // Default closure

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(Double leftMargin) {
        this.leftMargin = leftMargin;
    }

    public Double getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(Double rightMargin) {
        this.rightMargin = rightMargin;
    }

    public String getClosure() {
        return closure;
    }

    public void setClosure(String closure) {
        this.closure = closure;
    }
}
