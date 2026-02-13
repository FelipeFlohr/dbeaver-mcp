package dev.felipeflohr.dbeavermcp.module.ssh.manager;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NullMarked
@Service
class SSHTunnelManagerImpl implements SSHTunnelManager {
    private static final int CONNECTION_TIMEOUT = 20_000;
    private final Map<String, Session> activeTunnels = new ConcurrentHashMap<>();

    @Override
    public int openTunnel(String tunnelKey, String sshHost, int sshPort, String sshUser, String sshPassword, String remoteHost, int remotePort) throws DBeaverMCPValidationException {
        try {
            if (activeTunnels.containsKey(tunnelKey)) {
                Session session = activeTunnels.get(tunnelKey);
                if (session.isConnected()) {
                    log.info("Tunnel active for \"{}\". Using it.", tunnelKey);
                    return Integer.parseInt(session.getPortForwardingL()[0].split(":")[0]);
                }
                log.info("Tunnel \"{}\" found on the tunnels map, but it is not active. Removing it.", tunnelKey);
                activeTunnels.remove(tunnelKey);
            }

            var jsch = new JSch();
            Session session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(CONNECTION_TIMEOUT);

            int localPort = session.setPortForwardingL(0, remoteHost, remotePort);
            activeTunnels.put(tunnelKey, session);
            return localPort;
        } catch (JSchException e) {
            throw new DBeaverMCPValidationException("Failed to open SSH tunnel to %s:%d - %s".formatted(sshHost, sshPort, e.getMessage()), e);
        }
    }

    @PreDestroy
    private void closeAll() {
        int amountOfSessions = activeTunnels.size();
        if (amountOfSessions == 0) return;

        log.info("Closing {} session(s).", amountOfSessions);
        activeTunnels.values().forEach(Session::disconnect);
        log.info("Closed {} session(s).", amountOfSessions);
        activeTunnels.clear();
    }
}
