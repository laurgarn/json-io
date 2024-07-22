package com.cedarsoftware.io.sidesiterator;

import com.cedarsoftware.io.ObjectWriter;

import java.util.Iterator;
import java.util.Map;

public class StringKeyedMapIterator implements ObjectWriter.SidesIterator
{

    Iterator<? extends Map.Entry<String, ?>> underlying;
    String currentKey;

    public StringKeyedMapIterator(Map<String, ?> map)
    {
        underlying = map.entrySet().iterator();
    }

    public boolean hasNext()
    {
        return underlying.hasNext();
    }

    public Object next()
    {
        Map.Entry<String, ?> next = underlying.next();
        currentKey = next.getKey();
        return next.getValue();
    }

    public String currentKey()
    {
        return currentKey;
    }
}
