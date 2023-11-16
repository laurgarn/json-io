package com.cedarsoftware.util.io.sidesiterator;

import com.cedarsoftware.util.io.ObjectWriter;
import lombok.Getter;

import java.lang.reflect.Array;

public class ArrayIterator implements ObjectWriter.SidesIterator
{
    final Object object;
    @Getter
    private final boolean twoDeep;
    final int len;
    int i = 0;

    public ArrayIterator(Object array, boolean twoDeep)
    {
        object = array;
        len = Array.getLength(object);
        this.twoDeep = twoDeep;
    }

    public boolean hasNext()
    {
        return i < len;
    }

    public Object next()
    {
        return Array.get(object, i++);
    }

    public String currentKey()
    {
        return String.valueOf(i - 1);
    }
}
