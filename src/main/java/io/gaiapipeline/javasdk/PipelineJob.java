package io.gaiapipeline.javasdk;

import java.util.HashMap;

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
     * priority is the priority of execution (order) of this job.
     */
    private long priority;

    /**
     * args are the arguments passed by gaia for this job.
     */
    private HashMap<String, String> args;

    public PipelineJob(Handler handler, String title, String description, long priority, HashMap<String, String> args) {
        this.handler = handler;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.args = args;
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

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    public HashMap<String, String> getArgs() {
        return args;
    }

    public void setArgs(HashMap<String, String> args) {
        this.args = args;
    }
}
