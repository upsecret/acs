package com.acs.gateway.config;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import io.github.bucket4j.distributed.remote.RemoteBucketState;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProxyManager extends AbstractCompareAndSwapBasedProxyManager<String> {

    private final ConcurrentHashMap<String, byte[]> storage = new ConcurrentHashMap<>();

    public InMemoryProxyManager() {
        super(ClientSideConfig.getDefault());
    }

    @Override
    protected CompareAndSwapOperation beginCompareAndSwapOperation(String key) {
        return new CompareAndSwapOperation() {
            @Override
            public Optional<byte[]> getStateData(Optional<Long> timeNanos) {
                return Optional.ofNullable(storage.get(key));
            }

            @Override
            public boolean compareAndSwap(byte[] originalData, byte[] newData,
                                          RemoteBucketState newState, Optional<Long> timeNanos) {
                synchronized (storage) {
                    byte[] current = storage.get(key);
                    if (originalData == null && current == null) {
                        storage.put(key, newData);
                        return true;
                    }
                    if (originalData != null && Arrays.equals(current, originalData)) {
                        storage.put(key, newData);
                        return true;
                    }
                    return false;
                }
            }
        };
    }

    @Override
    protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(String key) {
        CompareAndSwapOperation syncOp = beginCompareAndSwapOperation(key);
        return new AsyncCompareAndSwapOperation() {
            @Override
            public CompletableFuture<Optional<byte[]>> getStateData(Optional<Long> timeNanos) {
                return CompletableFuture.completedFuture(syncOp.getStateData(timeNanos));
            }

            @Override
            public CompletableFuture<Boolean> compareAndSwap(byte[] originalData, byte[] newData,
                                                             RemoteBucketState newState, Optional<Long> timeNanos) {
                return CompletableFuture.completedFuture(
                        syncOp.compareAndSwap(originalData, newData, newState, timeNanos));
            }
        };
    }

    @Override
    public void removeProxy(String key) {
        storage.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(String key) {
        storage.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }
}
