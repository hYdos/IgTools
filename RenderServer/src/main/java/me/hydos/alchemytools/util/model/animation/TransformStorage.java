package me.hydos.alchemytools.util.model.animation;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

public class TransformStorage<T> implements Iterable<TransformStorage.TimeKey<T>> {
    public final TreeMap<Double, TimeKey<T>> keys = new TreeMap<>();
    public TimeKey<T>[] values = new TimeKey[0];

    @NotNull
    @Override
    public Iterator<TimeKey<T>> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return values.length > i;
            }

            @Override
            public TimeKey<T> next() {
                return values[i++];
            }
        };
    }

    public void add(double time, T value) {
        values = Arrays.copyOf(values, values.length + 1);
        var key = new TimeKey<>(time, values.length - 1, value);
        keys.put(time, key);
        values[values.length - 1] = key;
    }

    public TimeKey<T> get(int i) {
        if (i > values().length) return null;
        return values()[i];
    }

    public TimeKey<T> getAtTime(int animationDuration) {
        for (var value : values()) {
            if (value.time() == animationDuration) return value;
        }

        return null;
    }

    public int indexOf(TimeKey<T> value) {
        return value.idx;
    }

    public TimeKey<T> getBefore(TimeKey<T> valueAhead) {
        var index = indexOf(valueAhead);
        return get(Math.max(0, index - 1));
    }

    public TimeKey<T>[] values() {
        return values;
    }

    public int size() {
        return keys.size();
    }

    record TimeKey<T>(double time, int idx, T value) implements Comparable<TimeKey<T>> {

        @Override
        public int compareTo(@NotNull TransformStorage.TimeKey<T> timeKey2) {
            return Double.compare(time, timeKey2.time);
        }
    }
}
