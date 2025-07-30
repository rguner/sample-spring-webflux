package pl.piomin.services.controller;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.service.TraceService;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TraceDemoController {
    private static final Logger logger = LoggerFactory.getLogger(TraceDemoController.class);

    private final Tracer tracer;
    private final TraceService traceService;
    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();

    @GetMapping("/trace-demo")
    public Mono<String> traceDemo() {

        /*
        if (tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            String spanId = tracer.currentSpan().context().spanId();
            logger.info("Handling /trace-demo request {} {}", traceId, spanId);
        } else {
            logger.info("Handling /trace-demo request - no tracing context available");
        }

         */

        Span span = tracer.currentSpan() == null ? tracer.nextSpan() : tracer.currentSpan();
        String traceId = span.context().traceId();
        String spanId = span.context().spanId();
        logger.info("Handling /trace-demo request {} :  {}", traceId, spanId);

        String traceResponse = traceService.trace();
        return Mono.just(traceResponse);
    }

    @GetMapping("/span")
    public Mono<String> span() {
        return Mono.deferContextual(contextView -> {
            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY)) {
                String traceId = this.tracer.currentSpan().context().traceId();
                logger.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
                String traceResponse = traceService.trace();
                return Mono.just(traceId);
            }
        });
    }

    @GetMapping("/span2")
    public Mono<String> span2() {
        return Mono.deferContextual(contextView -> {
            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY)) {
                //String traceId = this.tracer.currentSpan().context().traceId();
                logger.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer");
                String traceResponse = traceService.trace();
                return Mono.just("Hello from span2");
            }
        });
    }
}
