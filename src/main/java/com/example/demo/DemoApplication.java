package com.example.demo;

import com.example.demo.emitter.SimpleEmitter;
import com.example.demo.listener.EventListener;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner test(SimpleEmitter simpleEmitter) {
        return (args) -> {
            Flux
                    .push(sink -> {
                        var listener = new EventListener<String>() {
                            @Override
                            public void onDataChunk(String chunk) {
                                sink.next(chunk);
                            }

                            @Override
                            public void onProcessComplete() {
                                sink.complete();
                            }
                        };
                        // появился потребитель, например, открыли новую вкладку в браузере
                        simpleEmitter.registerListener(listener);
                    }, FluxSink.OverflowStrategy.LATEST)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(item -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println(item);
                    });
        };
    }
}
