package io.gaiapipeline.javasdk;

import io.gaiapipeline.proto.Job;

public class JobsWrapper {

    /**
     * handler is the job implementation of one pipeline job.
     */
    private Handler handler;

    /**
     * job is gRPC representation of a job which will be transferred.
     */
    private Job job;

    public JobsWrapper(Handler handler, Job job) {
        this.handler = handler;
        this.job = job;
    }

    public JobsWrapper() {
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}
