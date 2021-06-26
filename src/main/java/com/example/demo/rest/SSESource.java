package com.example.demo.rest;

import com.example.demo.emitter.SimpleEmitter;
import com.example.demo.listener.EventListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RestController
public class SSESource implements InitializingBean {

    private final SimpleEmitter simpleEmitter;

    private Flux<String> bridge;

    public SSESource(SimpleEmitter simpleEmitter) {
        this.simpleEmitter = simpleEmitter;
    }

    @GetMapping(path = "/stream-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamFlux() {
        return bridge.doOnNext(item -> {
            //System.out.println(item);
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //push - работает только для одногопоточного producer!!!
        bridge = Flux.push(
                sink -> {
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

                    // когда потребитель уже не ожидает данные, удаляем его из слушателей (закрыли вкладку браузера)
                    sink.onDispose(() -> simpleEmitter.deleteListener(listener));
                }, FluxSink.OverflowStrategy.LATEST);

        // если не перебросить обработку отправки на другие потоки, то поток, генерирующий события,
        // будет сам отправлять события - то есть будет синхронным, а с этим пробросом,
        // то вызов станет асинхронным
        bridge = bridge.publishOn(Schedulers.boundedElastic());
    }
}
