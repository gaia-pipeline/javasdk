package io.gaiapipeline.javasdk;

import java.util.ArrayList;

public class PipelineJob {

    /**
     * handler is the handler which will be executed on pipeline execution.
     */
    private Handler handler;

    /**
     * title is the title used for this single job.
     */
    private String title;

    /**
     * description is the description for this single job.
     */
    private String description;

    /**
     * dependsOn is a list of job titles. Dependent jobs will be
     * executed before the job self.
     */
    private ArrayList<String> dependsOn;

    /**
     * args are the arguments passed by gaia for this job.
     */
    private ArrayList<PipelineArgument> args;

    /**
     * interaction is a manual interaction shown to the user right before job execution.
     */
    private PipelineManualInteraction interaction;

    public PipelineJob(Handler handler, String title, String description) {
        this.handler = handler;
        this.title = title;
        this.description = description;
    }

    public PipelineJob(Handler handler, String title, String description, ArrayList<String> dependsOn, ArrayList<PipelineArgument> args) {
        this.handler = handler;
        this.title = title;
        this.description = description;
        this.dependsOn = dependsOn;
        this.args = args;
    }

    public PipelineJob(Handler handler, String title, String description, ArrayList<String> dependsOn, ArrayList<PipelineArgument> args, PipelineManualInteraction interaction) {
        this.handler = handler;
        this.title = title;
        this.description = description;
        this.dependsOn = dependsOn;
        this.args = args;
        this.interaction = interaction;
    }

    public PipelineJob() {
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(ArrayList<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public ArrayList<PipelineArgument> getArgs() {
        return args;
    }

    public void setArgs(ArrayList<PipelineArgument> args) {
        this.args = args;
    }

    public PipelineManualInteraction getInteraction() {
        return interaction;
    }

    public void setInteraction(PipelineManualInteraction interaction) {
        this.interaction = interaction;
    }
}
