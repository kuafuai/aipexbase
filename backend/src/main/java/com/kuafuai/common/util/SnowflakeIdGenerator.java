package com.kuafuai.common.util;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Random;

@Component
public class SnowflakeIdGenerator {

    private  long workerId;
    private  long datacenterId;
    private final long sequence = 0L;

    private final long twepoch = 1288834974657L; // 自定义起始时间戳

    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;
    private long currentSequence = 0L;
    private final Random random = new Random();

    public SnowflakeIdGenerator() {
    }


    @PostConstruct
    public void init() {
        // 随机初始化 workerId 和 datacenterId
        this.workerId = random.nextInt((int) (maxWorkerId + 1));       // 0 ~ 31
        this.datacenterId = random.nextInt((int) (maxDatacenterId + 1)); // 0 ~ 31

    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("时钟回拨。拒绝生成 id，当前时间 %d 小于上次时间戳 %d", timestamp, lastTimestamp));
        }

        if (lastTimestamp == timestamp) {
            currentSequence = (currentSequence + 1) & sequenceMask;
            if (currentSequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            currentSequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                currentSequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

}
