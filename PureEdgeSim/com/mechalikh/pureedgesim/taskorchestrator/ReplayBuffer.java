package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.LinkedList;
import java.util.Queue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReplayBuffer implements Serializable{
    private static final long serialVersionUID = 1L; // Consigliato per la serializzazione

    private Queue<Experience> buffer;
    private int maxSize;

    public ReplayBuffer(int size) {
        this.buffer = new LinkedList<>();
        this.maxSize = size;
    }

    public void add(Experience experience) {
        if (buffer.size() >= maxSize) {
            buffer.poll();
        }
        buffer.add(experience);
    }

    public List<Experience> sample(int batchSize) {
        List<Experience> sample = new ArrayList<>();
        Random random = new Random();
        List<Experience> bufferList = new ArrayList<>(buffer);
        for (int i = 0; i < batchSize; i++) {
            int index = random.nextInt(buffer.size());
            sample.add(bufferList.get(index));
        }
        return sample;
    }

    public int size() {
        return buffer.size();
    }
}