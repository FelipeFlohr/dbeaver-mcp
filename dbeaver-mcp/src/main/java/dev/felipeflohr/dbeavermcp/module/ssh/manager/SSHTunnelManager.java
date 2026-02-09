package dev.felipeflohr.dbeavermcp.module.ssh.manager;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SSHTunnelManager {
    int openTunnel(
            String tunnelKey,
            String sshHost,
            int sshPort,
            String sshUser,
            String sshPassword,
            String remoteHost,
            int remotePort
    ) throws DBeaverMCPValidationException;
}
