package io.gaiapipeline.javasdk;

/**
 * InputType defines the available input types in the UI.
 * Use input type "VaultInp" to get parameters from the vault.
 */
public enum InputType {
    TextFieldInp("textfield"), TextAreaInp("textarea"), BoolInp("boolean"), VaultInp("vault");

    private final String type;

    InputType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}