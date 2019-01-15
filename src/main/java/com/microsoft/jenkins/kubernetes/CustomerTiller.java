package com.microsoft.jenkins.kubernetes;

import io.fabric8.kubernetes.client.HttpClientAware;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.microbean.development.annotation.Issue;
import org.microbean.helm.Tiller;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CustomerTiller extends Tiller {
    private static final int IDLE_TIMEOUT = 5;
    private static final int KEEP_ALIVE_TIME = 30;

    public CustomerTiller(ManagedChannel channel) {
        super(channel);
    }

    public <T extends HttpClientAware & KubernetesClient>
    CustomerTiller(T client, String namespaceHousingTiller) throws MalformedURLException {
        super(client, namespaceHousingTiller);
    }

    /**
     * Overwrite the method of {@link org.microbean.helm.Tiller#buildChannel(LocalPortForward)}
     * to disable the trace of census which will cause Linkage error.
     *
     * @param portForward port bound to
     * @return communicate channel
     */
    @Override
    @Issue(id = "42", uri = "https://github.com/microbean/microbean-helm/issues/42")
    public ManagedChannel buildChannel(LocalPortForward portForward) {
        Objects.requireNonNull(portForward);
        @Issue(id = "43", uri = "https://github.com/microbean/microbean-helm/issues/43") final InetAddress localAddress
                = portForward.getLocalAddress();
        if (localAddress == null) {
            throw new IllegalArgumentException("portForward",
                    new IllegalStateException("portForward.getLocalAddress() == null"));
        }
        final String hostAddress = localAddress.getHostAddress();
        if (hostAddress == null) {
            throw new IllegalArgumentException("portForward",
                    new IllegalStateException("portForward.getLocalAddress().getHostAddress() == null"));
        }
        return ManagedChannelBuilder.forAddress(hostAddress, portForward.getLocalPort())
                .enableRetry()
                .idleTimeout(IDLE_TIMEOUT, TimeUnit.SECONDS)
                .keepAliveTime(KEEP_ALIVE_TIME, TimeUnit.SECONDS)
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .usePlaintext(true)
                .build();
    }
}
