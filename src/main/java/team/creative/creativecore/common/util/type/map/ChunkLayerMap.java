package team.creative.creativecore.common.util.type.map;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.function.Function;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkLayerMap<T> extends EnumMap<ChunkSectionLayer, T> implements Iterable<T> {
    public ChunkLayerMap(ChunkLayerMap<T> map) {
        super(map);
    }
    
    public ChunkLayerMap(Function<ChunkSectionLayer, T> factory) {
        super(ChunkSectionLayer.class);
        for(ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            super.put(layer, factory.apply(layer));
        }
    }
    
    public ChunkLayerMap() {
        super(ChunkSectionLayer.class);
    }
    
    public Iterable<Entry<ChunkSectionLayer, T>> tuples() {
        return super.entrySet();
    }
    
    @Override
    public Iterator<T> iterator() {
        return super.values().iterator();
    }
}
