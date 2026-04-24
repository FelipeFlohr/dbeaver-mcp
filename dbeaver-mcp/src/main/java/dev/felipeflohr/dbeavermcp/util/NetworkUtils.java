package dev.felipeflohr.dbeavermcp.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@UtilityClass
public class NetworkUtils {
    public boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
