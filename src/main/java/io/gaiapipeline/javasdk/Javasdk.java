package io.gaiapipeline.javasdk;

import io.gaiapipeline.proto.*;
import io.grpc.Server;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Gaia - Javasdk
 *
 * Use this SDK to build powerful pipelines for Gaia.
 * For help, have a look at our docs: https://docs.gaia-pipeline.io
 *
 */
public class Javasdk
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
    private static final int PROTOCOL_VERSION = 2;

    /**
     * Protocol name.
     * Do not change unless you know what to do.
     */
    private static final String PROTOCOL_NAME = "tcp";

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
     * The listen address where the server is listening to.
     */
    private static final String LISTEN_ADDR = "localhost";

    /**
     * Helper variables to pass certificates path.
     */
    private final String certChainFilePath;
    private final String privateKeyFilePath;
    private final String trustCertCollectionFilePath;

    private static final Logger logger = Logger.getLogger(Javasdk.class.getName());

    public Javasdk() {
        this.certChainFilePath = System.getenv("GAIA_PLUGIN_CERT");
        this.privateKeyFilePath = System.getenv("GAIA_PLUGIN_KEY");
        this.trustCertCollectionFilePath = System.getenv("GAIA_PLUGIN_CA_CERT");
    }

    /**
     * Serve initiates the gRPC Server and listens for incoming traffic.
     * This method is blocking forever and should be last called.
     * @param jobs - List of jobs which are available for execution
     * @throws Exception - Throws an exception if something goes wrong
     */
    public void Serve(ArrayList<PipelineJob> jobs) throws Exception {
        // Surround all jobs with a wrapper for later processing
        cachedJobs = new ArrayList<>();
        for (PipelineJob job: jobs) {
            // Manual interaction
            ManualInteraction.Builder maBuilder = ManualInteraction.newBuilder();
            if (job.getInteraction() != null) {
                maBuilder.setDescription(job.getInteraction().getDescription());
                maBuilder.setType(job.getInteraction().getType().getType());
                maBuilder.setValue(job.getInteraction().getValue());
            }

            // Arguments
            ArrayList<Argument> args = new ArrayList<>();
            if (job.getArgs() != null) {
                for (PipelineArgument arg: job.getArgs()) {
                    Argument protoArg = Argument.newBuilder()
                            .setDescription(arg.getDescription())
                            .setType(arg.getType().getType())
                            .setKey(arg.getKey())
                            .setValue(arg.getValue())
                            .build();

                    args.add(protoArg);
                }
            }

            // Resolve dependencies
            ArrayList<Integer> dependson = new ArrayList<>();
            if (job.getDependsOn() != null) {
                for (String depJob: job.getDependsOn()) {
                    boolean foundDep = false;
                    for (PipelineJob currJob: jobs) {
                        if (depJob.equalsIgnoreCase(currJob.getTitle())) {
                            dependson.add(getHash(currJob.getTitle()));
                            foundDep = true;
                            break;
                        }
                    }

                    if (!foundDep) {
                        throw new Exception("job '" + job.getTitle() + "' has dependency '" + depJob + "' which is not declared!");
                    }
                }
            }

            // Create gRPC plugin object
            Job grpcJob = Job.newBuilder().
                    setUniqueId(getHash(job.getTitle()))
                    .setTitle(job.getTitle())
                    .setDescription(job.getDescription())
                    .setInteraction(maBuilder.build())
                    .addAllArgs(args)
                    .addAllDependson(dependson)
                    .build();

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

        // Create health manager
        HealthStatusManager health = new HealthStatusManager();
        health.setStatus("plugin", HealthCheckResponse.ServingStatus.SERVING);

        // Create socket address
        InetSocketAddress socketAddr = new InetSocketAddress(LISTEN_ADDR, 0);

        // Build and start server
        Server server = NettyServerBuilder.forAddress(socketAddr)
                .addService(health.getHealthService())
                .addService(ProtoReflectionService.newInstance())
                .addService(new PluginImpl())
                .sslContext(getSslContextBuilder().build())
                .build().start();

        // Output the address and service name to stdout.
        // hasicorp go-plugin will use that to establish connection.
        String connectString = CORE_PROTOCOL_VERSION + "|" +
                PROTOCOL_VERSION + "|" +
                PROTOCOL_NAME + "|" +
                socketAddr.getHostName() + ":" + server.getPort() + "|" +
                PROTOCOL_TYPE + "\n";
        System.out.print(connectString);

        // Listen to shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Stop server
                server.shutdown();
            }
        });

        // Wait until terminated
        server.awaitTermination();
    }

    private SslContextBuilder getSslContextBuilder() {
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(certChainFilePath),
                new File(privateKeyFilePath));
        sslClientContextBuilder.trustManager(new File(trustCertCollectionFilePath));
        sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
        return GrpcSslContexts.configure(sslClientContextBuilder,
                SslProvider.OPENSSL);
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
            JobsWrapper jobWrap = null;
            for (JobsWrapper jobWrapper: cachedJobs) {
                if (jobWrapper.getJob().getUniqueId() == job.getUniqueId()) {
                    jobWrap = jobWrapper;
                    break;
                }
            }

            // We couldn't found the job in our cache return error
            if (jobWrap == null) {
                logger.info("cannot find job " + job.getTitle() + " in pipeline");
                stream.onError(new Exception("job not found in plugin"));
                return;
            }

            // Transform arguments
            ArrayList<PipelineArgument> args = new ArrayList<>();
            for (Argument arg: job.getArgsList()) {
                PipelineArgument pArg = new PipelineArgument(
                        arg.getKey(),
                        arg.getValue()
                );

                args.add(pArg);
            }

            // Execute job
            JobResult.Builder resultBuilder = JobResult.newBuilder();
            try {
                jobWrap.getHandler().executeHandler(args);
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
