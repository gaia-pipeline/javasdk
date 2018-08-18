package io.gaiapipeline.javasdk;

public class PipelineManualInteraction {

    /**
     * description is the description displayed in the UI.
     */
    private String description = "";

    /**
     * type is the input type used in the UI.
     */
    private InputType type = InputType.TextFieldInp;

    /**
     * value is the value provided by the user.
     */
    private String value = "";

    public PipelineManualInteraction() {
    }

    public PipelineManualInteraction(String description, InputType type) {
        this.description = description;
        this.type = type;
    }

    public PipelineManualInteraction(String description, InputType type, String value) {
        this.description = description;
        this.type = type;
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InputType getType() {
        return type;
    }

    public void setType(InputType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
