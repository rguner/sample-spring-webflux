package pl.piomin.services.controller;

import java.sql.Time;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.piomin.services.model.Person;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/persons")
public class PersonController {

    @Autowired
    @Qualifier("subscriberTaskExecutor")
    ThreadPoolTaskExecutor subscriberTaskExecutor;

    @Autowired
    @Qualifier("publisherTaskExecutor")
    ThreadPoolTaskExecutor publisherTaskExecutor;

    @Autowired
    WebClient client;

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonController.class);

    @GetMapping("/json")
    public Flux<Person> findPersonsJson() {
        LOGGER.info("Http Request findPersonsJson");
        Flux<Person> flux = Flux.fromStream(this::prepareStream)
                //.doOnNext(person -> LOGGER.info("Server produces: {}", person));
                // publish, subscribe ikisi de çalışıyor, ancak ikisi birlikte set edilirse publisher çalışıyor.
                //.publishOn(Schedulers.fromExecutor(publisherTaskExecutor))
                .subscribeOn(Schedulers.fromExecutor(subscriberTaskExecutor))
                .log();
        flux.subscribe(p-> LOGGER.info("Person: {}", p));
        LOGGER.info("Http Request findPersonsJson finished");
        return flux;
    }

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<Person> findPersonsStream() {
        LOGGER.info("Http Request findPersonsStream");
        Flux<Person> flux = Flux.fromStream(this::prepareStream).delaySequence(Duration.ofMillis(100))
                //.doOnNext(person -> LOGGER.info("Server produces: {}", person));
                .log();

        // flux.subscribe(System.out::println);

        /*
        flux.subscribe(p -> {
            System.out.println(p.getFirstName() + " " + Thread.currentThread().getName());
            try {
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        */
        LOGGER.info("Http Request findPersonsStream finished");
        return flux;
    }

    @GetMapping(value = "/stream/back-pressure", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<Person> findPersonsStreamBackPressure() {
        return Flux.fromStream(this::prepareStream).delayElements(Duration.ofMillis(100))
                .doOnNext(person -> LOGGER.info("Server produces: {}", person))
                ;
                //.log();
    }

    private Stream<Person> prepareStream() {
        return Stream.of(
            new Person(1, "Name01", "Surname01", 11),
            new Person(2, "Name02", "Surname02", 22),
            new Person(3, "Name03", "Surname03", 33),
            new Person(4, "Name04", "Surname04", 44),
            new Person(5, "Name05", "Surname05", 55),
            new Person(6, "Name06", "Surname06", 66),
            new Person(7, "Name07", "Surname07", 77),
            new Person(8, "Name08", "Surname08", 88),
            new Person(9, "Name09", "Surname09", 99)
        );
    }

	private Stream<Person> prepareStreamPart1() {
		return Stream.of(
				new Person(1, "Name01", "Surname01", 11),
				new Person(2, "Name02", "Surname02", 22),
				new Person(3, "Name03", "Surname03", 33)
		);
	}

    // http://localhost:8080/persons/integration/RAMAZAN
	@GetMapping("/integration/{param}")
    public Flux<Person> findPersonsIntegration(@PathVariable("param") String param) {
        LOGGER.info("Http Request findPersonsIntegration");
        Flux<Person> flux = Flux.fromStream(this::prepareStreamPart1).log()
                .mergeWith(
                        client.get().uri("/slow/" + param)
                                .retrieve()
                                .bodyToFlux(Person.class)
                                .log()
                );
        LOGGER.info("Http Request findPersonsIntegration finished");
        return flux;
    }

    @GetMapping("/integration-in-different-pool/{param}")
    public Flux<Person> findPersonsIntegrationInDifferentPool(@PathVariable("param") String param) {
        LOGGER.info("Http Request findPersonsIntegrationInDifferentPool");
        /*
        Flux<Person> flux = Flux.fromStream(this::prepareStreamPart1).log()
                .mergeWith(
                        client.get().uri("/slow/" + param)
                                .retrieve()
                                .bodyToFlux(Person.class)
                                .log()
                                //.publishOn(Schedulers.fromExecutor(publisherTaskExecutor))
                                //.subscribeOn(Schedulers.fromExecutor(subscriberTaskExecutor))
                )
                .subscribeOn(Schedulers.fromExecutor(subscriberTaskExecutor));
        */
        // subscribeOn(Schedulers.fromExecutor(subscriberTaskExecutor)); aşağıdaki işlemde bir işe yaramadı
        Flux<Person> flux = client.get().uri("/slow/" + param)
                .retrieve()
                .bodyToFlux(Person.class)
                .publishOn(Schedulers.fromExecutor(publisherTaskExecutor))
                .log()
                ;


        LOGGER.info("Http Request findPersonsIntegrationInDifferentPool finished");
        return flux;
    }

}
