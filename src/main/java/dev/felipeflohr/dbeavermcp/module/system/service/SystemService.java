package dev.felipeflohr.dbeavermcp.module.system.service;

import dev.felipeflohr.dbeavermcp.module.system.enumerator.OS;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SystemService {
    OS getOS();
}
