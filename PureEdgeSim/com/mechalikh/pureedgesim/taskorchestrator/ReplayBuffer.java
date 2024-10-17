package com.mechalikh.pureedgesim.taskorchestrator;

import java.util.LinkedList;
import java.util.Queue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

    // Questa versione permette la ripetizione di indici già presenti nel batch, però è più veloce avendo una complessità computazionale inferiore rispetto alla seconda
    // public List<Experience> sample(int batchSize) {
    //     List<Experience> sample = new ArrayList<>();
    //     Random random = new Random();
    //     List<Experience> bufferList = new ArrayList<>(buffer);
    //     for (int i = 0; i < batchSize; i++) {
    //         int index = random.nextInt(buffer.size());
    //         sample.add(bufferList.get(index));
    //     }
    //     return sample;
    // }

    // Questa versione assicura di non campionare più volte gli stessi indici, d'altro canto  è caratterizzata da una complessità maggiore O(n)
    public List<Experience> sample(int batchSize) {
        List<Experience> bufferList = new ArrayList<>(buffer);
        Collections.shuffle(bufferList);
        return bufferList.subList(0, Math.min(batchSize, bufferList.size()));
    }


    public int size() {
        return buffer.size();
    }
}