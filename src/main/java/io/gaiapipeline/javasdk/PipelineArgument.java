package io.gaiapipeline.javasdk;

public class PipelineArgument {

    /**
     * description is the description displayed in the UI.
     */
    private String description = "";

    /**
     * type is the argument type used in the UI.
     */
    private InputType type = InputType.TextFieldInp;

    /**
     * key is the key used to identify the argument.
     */
    private String key = "";

    /**
     * value contains the argument value from the user.
     */
    private String value = "";

    public PipelineArgument(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public PipelineArgument(String description, InputType type, String key, String value) {
        this.description = description;
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public PipelineArgument() {
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
