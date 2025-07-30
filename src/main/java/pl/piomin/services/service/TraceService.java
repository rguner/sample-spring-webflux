package pl.piomin.services.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TraceService {

    public String trace() {
        log.info("Hello from Trace Service!");
        return "Trace";
    }
}
