package citrea.swarm4j.server;

import citrea.swarm4j.core.model.Host;
import citrea.swarm4j.core.SwarmException;
import citrea.swarm4j.core.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * @author aleksisha
 * Date: 25/10/13
 * Time: 16:47
 */
@Component
public class SwarmServer {
    public final Logger logger = LoggerFactory.getLogger(SwarmServer.class.getName());

    @Autowired
    private Utils utils;

    @Value("${port:8080}")
    private int port;

    @Value("${decoders:1}")
    private int decoders = 1;

    private WSServerImpl wsServer;
    private Host host;

    public SwarmServer() {

    }

    public void setHost(Host host) {
        this.host = host;
    }

    public void stop() throws IOException, InterruptedException, SwarmException {
        logger.info("stopping");
        wsServer.stop();
        host.stop();
    }

    public void start() throws SwarmException {
        if (host == null) {
            throw new RuntimeException("'host' property not deliver");
        }
        host.start();
        wsServer = new WSServerImpl(port, decoders, host, utils);
        wsServer.start();
        logger.info("started on port: " + port);
    }
}