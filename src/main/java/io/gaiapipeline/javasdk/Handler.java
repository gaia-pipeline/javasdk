package io.gaiapipeline.javasdk;

import java.util.ArrayList;

public interface Handler {

    void executeHandler(ArrayList<PipelineArgument> args) throws Exception;

}
