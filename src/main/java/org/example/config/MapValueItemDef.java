package org.example.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MapValueItemDef {
    @JsonProperty(required = true)
    private Object input; // The original value to match

    @JsonProperty(required = true)
    private Object output; // The value to map to

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }
}
