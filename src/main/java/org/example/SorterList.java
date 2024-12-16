package org.example;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

// Sorts the list indexes based on the order of the elements in the list.
public class SorterList<T> implements Comparator<Integer> {

    private final @NotNull Sorter sorter;
    private final @NotNull List<T> objects;

    public SorterList(@NotNull Sorter s, @NotNull List<T> objects) {
        this.sorter = s;
        this.objects = objects;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        return sorter.compare(objects.get(o1), objects.get(o2));
    }
}
