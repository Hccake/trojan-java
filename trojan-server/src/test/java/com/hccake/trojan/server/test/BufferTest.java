package com.hccake.trojan.server.test;

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAllocator;
import io.netty5.buffer.DefaultBufferAllocators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author hccake
 */
class BufferTest {

    /**
     * WIKI 上写的 Buffer 不会再自动扩容，实际代码是会的，简单测试下
     */
    @Test
    void testAutomaticallyGrowCapacity() {
        BufferAllocator bufferAllocator = DefaultBufferAllocators.onHeapAllocator();
        Buffer buffer = bufferAllocator.allocate(1);

        buffer.writeByte((byte) 1);
        buffer.writeByte((byte) 2);
        buffer.writeByte((byte) 3);
        buffer.writeByte((byte) 4);
        buffer.writeByte((byte) 5);

        Assertions.assertEquals(5, buffer.readableBytes());
    }

}
