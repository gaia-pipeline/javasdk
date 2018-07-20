package io.gaiapipeline.javasdk;

import java.util.Map;

public interface Handler {

    void executeHandler(Map<String, String> args) throws Exception;

}
