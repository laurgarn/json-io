package com.cedarsoftware.io.sidesiterator;

import com.cedarsoftware.io.ObjectWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class KeysThenValuesMapIterator implements ObjectWriter.SidesIterator
{

    public static final Object KEY_CONTEXT = "Key Context";
    public static final Object VALUE_CONTEXT = "Value Context";

    private final Iterator<?> keysIterator;
    private final Iterator<?> valuesIterator;

    private Object prevContext;
    private Object currentContext;

    public KeysThenValuesMapIterator(Map<?, ?> map)
    {
        int size = map.size();
        ArrayList<Object> keys = new ArrayList<>(size);
        ArrayList<Object> values = new ArrayList<>(size);

        for (Map.Entry<?, ?> p : map.entrySet())
        {
            keys.add(p.getKey());
            values.add(p.getValue());
        }

        keysIterator = keys.iterator();
        valuesIterator = values.iterator();
    }

    public boolean areKeysDone()
    {
        return !keysIterator.hasNext();
    }

    public boolean hasNext()
    {
        if (keysIterator.hasNext())
        {
            return true;
        }

        boolean ret = valuesIterator.hasNext();
        if (!ret)
        {
            currentContext = null;
        }
        return ret;
    }

    public Object next()
    {
        prevContext = currentContext;
        if (keysIterator.hasNext())
        {
            currentContext = KEY_CONTEXT;
            return keysIterator.next();
        }
        currentContext = VALUE_CONTEXT;
        return valuesIterator.next();
    }

    public Object prevContext()
    {
        return prevContext;
    }

    public Object currentContext()
    {
        return currentContext;
    }
}
