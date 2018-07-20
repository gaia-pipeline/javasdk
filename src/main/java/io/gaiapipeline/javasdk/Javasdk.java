package io.gaiapipeline.javasdk;

import io.gaiapipeline.proto.Empty;
import io.gaiapipeline.proto.Job;
import io.gaiapipeline.proto.JobResult;
import io.gaiapipeline.proto.PluginGrpc;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

import java.io.File;
import java.util.ArrayList;

/**
 * Gaia - Javasdk
 *
 * Use this SDK to build powerful pipelines for Gaia.
 * For help, have a look at our docs: https://docs.gaia-pipeline.io
 *
 */
public final class Javasdk
{
    /**
     * Core protocol version.
     * Do not change unless you know what you do.
     */
    private static final int CORE_PROTOCOL_VERSION = 1;

    /**
     * Protocol version.
     * Do not change unless you know what you do.
     */
    private static final int PROTOCOL_VERSION = 1;

    /**
     * Protocol name.
     * Do not change unless you know what to do.
     */
    private static final String PROTOCOL_NAME = "unix";

    /**
     * Protocol type.
     * Do not change unless you know what you do.
     */
    private static final String PROTOCOL_TYPE = "grpc";

    /**
     * cachedJobs are the injected jobs from the pipeline implementation.
     */
    private static ArrayList<JobsWrapper> cachedJobs;

    /**
     * FNV values for string 32bit hashing.
     */
    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;

    /**
     * Serve initiates the gRPC Server and listens for incoming traffic.
     * This method is blocking forever and should be last called.
     * @param jobs - List of jobs which are available for execution
     * @throws Exception - Throws an exception if something goes wrong
     */
    public void Serve(ArrayList<PipelineJob> jobs) throws Exception {
        // Surround all jobs with a wrapper for later processing
        cachedJobs = new ArrayList<JobsWrapper>();
        for (PipelineJob job: jobs) {
            // Create gRPC plugin object
            Job grpcJob = Job.newBuilder().
                    setUniqueId(getHash(job.getTitle()))
                    .setTitle(job.getTitle())
                    .setDescription(job.getDescription())
                    .setPriority(job.getPriority()).build();

            // Create wrapper around gRPC plugin object
            JobsWrapper wrapper = new JobsWrapper();
            wrapper.setJob(grpcJob);
            wrapper.setHandler(job.getHandler());

            // Add it to array list
            cachedJobs.add(wrapper);
        }

        // Check if two jobs have the same title which is restricted.
        for (int x=0; x<cachedJobs.size(); x++) {
            for (int y = 0; y<cachedJobs.size(); y++) {
                if (x != y && cachedJobs.get(x).getJob().getUniqueId() == cachedJobs.get(y).getJob().getUniqueId()) {
                    throw new Exception("duplicate job: At least two jobs with the same title found. This is not allowed!");
                }
            }
        }

        // Create a temp file for the unix socket
        File tempFile = File.createTempFile("gaia", "plugin");

        // We delete it directly cause we don't need it to be existent.
        tempFile.delete();

        // Create health manager
        HealthStatusManager health = new HealthStatusManager();
        health.setStatus("plugin", HealthCheckResponse.ServingStatus.SERVING);

        // Create socket connection
        EpollEventLoopGroup group = new EpollEventLoopGroup();
        final Server server = NettyServerBuilder.forAddress(new DomainSocketAddress(tempFile.getAbsolutePath()))
                .channelType(EpollServerDomainSocketChannel.class)
                .workerEventLoopGroup(group)
                .bossEventLoopGroup(group)
                .addService(new PluginImpl())
                .addService(health.getHealthService())
                .addService(ProtoReflectionService.newInstance())
                .build();

        // Output the address and service name to stdout.
        // hasicorp go-plugin will use that to establish connection.
        String connectString = CORE_PROTOCOL_VERSION + "|" +
                PROTOCOL_VERSION + "|" +
                PROTOCOL_NAME + "|" +
                tempFile.getAbsolutePath() + "|" +
                PROTOCOL_TYPE;
        System.out.print(connectString);

        // Start gRPC server
        server.start();

        // Listen to shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Stop server
                server.shutdown();
            }
        });

    }

    /**
     * getHash takes a string and transforms it into a 32bit hash
     * @param s - String which should be transformed
     * @return - 32bit hash of the string
     */
    private static int getHash(String s) {
        int rv = FNV_32_INIT;
        final int len = s.length();
        for(int i = 0; i < len; i++) {
            rv ^= s.charAt(i);
            rv *= FNV_32_PRIME;
        }
        return rv;
    }

    /**
     * PluginImpl is the real server implementation of PluginGrpc.
     */
    static class PluginImpl extends PluginGrpc.PluginImplBase {

        /**
         * getJobs returns all available jobs from the pipeline.
         * @param empty - Empty object.
         * @param stream - Stream used to send jobs back.
         */
        @Override
        public void getJobs(Empty empty, StreamObserver<Job> stream) {
            for (JobsWrapper job: cachedJobs) {
                stream.onNext(job.getJob());
            }
            stream.onCompleted();
        }

        /**
         * executeJob executes the given job and returns the results of this job.
         * @param job - The job which should be executed
         * @param stream - Stream used to return the result object
         */
        @Override
        public void executeJob(Job job, StreamObserver<JobResult> stream) {
            // Find job object in our job cache
            int hashedTitle = Javasdk.getHash(job.getTitle());
            JobsWrapper jobWrap = null;
            for (JobsWrapper jobWrapper: cachedJobs) {
                if (jobWrapper.getJob().getUniqueId() == hashedTitle) {
                    jobWrap = jobWrapper;
                    break;
                }
            }

            // We couldn't found the job in our cache return error
            if (jobWrap == null) {
                stream.onError(new Exception("job not found in plugin"));
                stream.onCompleted();
                return;
            }

            // Execute job
            JobResult.Builder resultBuilder = JobResult.newBuilder();
            try {
                jobWrap.getHandler().executeHandler(job.getArgsMap());
            } catch (Exception ex) {
                if (ex instanceof ExitPipelineException) {
                    resultBuilder.setExitPipeline(true);
                } else {
                    resultBuilder.setExitPipeline(true);
                    resultBuilder.setFailed(true);
                }

                // Set log message and id
                resultBuilder.setUniqueId(jobWrap.getJob().getUniqueId());
                resultBuilder.setMessage(ex.getMessage());
            }

            // Return result
            stream.onNext(resultBuilder.build());
            stream.onCompleted();
        }

    }

}
