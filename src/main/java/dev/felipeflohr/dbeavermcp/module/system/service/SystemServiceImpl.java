package dev.felipeflohr.dbeavermcp.module.system.service;

import dev.felipeflohr.dbeavermcp.module.system.enumerator.OS;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

@NullMarked
@Service
class SystemServiceImpl implements SystemService {
    @Override
    public OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return OS.WINDOWS;
        if (os.contains("mac")) return OS.MAC;
        return OS.LINUX;
    }
}
