package com.example.demo.emitter;

import com.example.demo.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Эмулятор событий - делаем в одном потоке отправку данных вверх слушателям
 */
@Service
public class SimpleEmitter implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(SimpleEmitter.class);
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final List<EventListener<String>> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isRun = true;

    private final Runnable runnable = () -> {
        try {
            var event = "Simple_";
            long counter = 0;
            while (isRun) {

                for (var listener : listeners) {
                    listener.onDataChunk(event + counter);
                }

                counter++;
                Thread.sleep(500);
            }
            for (var listener : listeners) {
                listener.onProcessComplete();
            }

        } catch (InterruptedException ex) {
            for (var listener : listeners) {
                listener.onProcessComplete();
            }

            logger.warn("Thread {} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        } catch (RuntimeException exception) {
            logger.error("Internal error", exception);
        }
    };

    public void registerListener(EventListener listener) {
        listeners.add(listener);
    }

    public void deleteListener(EventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService.execute(runnable);
    }

    @Override
    public void destroy() throws Exception {
        isRun = false;
        listeners.clear();
        shutdownAndAwaitTermination();
    }

    private void shutdownAndAwaitTermination() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
