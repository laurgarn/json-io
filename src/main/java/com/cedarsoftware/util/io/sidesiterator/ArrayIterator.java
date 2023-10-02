package com.cedarsoftware.util.io.sidesiterator;

import com.cedarsoftware.util.io.ObjectWriter;

import java.lang.reflect.Array;

public class ArrayIterator implements ObjectWriter.SidesIterator
{
    final Object object;
    final int len;
    int i = 0;

    public ArrayIterator(Object array)
    {
        object = array;
        len = Array.getLength(object);
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
