package com.example.demo.listener;

public interface EventListener<T> {
    void onDataChunk(T chunk);

    void onProcessComplete();
}
