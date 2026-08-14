package team.creative.creativecore.common.util.type.map;

import java.util.*;
import java.util.function.BiConsumer;

import com.google.common.collect.Iterators;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkLayerMapList<T> extends EnumMap<ChunkSectionLayer, List<T>> implements Iterable<T> {
    public ChunkLayerMapList(ChunkLayerMapList<T> map) {
        super(map);
    }
    
    public ChunkLayerMapList() {
        super(ChunkSectionLayer.class);
    }
    
    public List<T> getOrCreate(ChunkSectionLayer layer) {
        return super.computeIfAbsent(layer, _ -> new ArrayList<>());
    }
    
    public void add(ChunkSectionLayer layer, T element) {
        getOrCreate(layer).add(element);
    }
    
    public void consumeEachLayer(BiConsumer<ChunkSectionLayer, List<T>> consumer) {
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            var list = new ArrayList<T>();
            consumer.accept(layer, list);
            super.put(layer, list);
        }
    }

    public Iterable<Entry<ChunkSectionLayer, List<T>>> tuples() {
        return super.entrySet();
    }
    
    @Override
    public Iterator<T> iterator() {
        return Iterators.concat(Iterators.transform(super.values().iterator(), List::iterator));
    }
}
