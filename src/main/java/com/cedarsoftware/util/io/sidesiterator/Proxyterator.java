package com.cedarsoftware.util.io.sidesiterator;

import com.cedarsoftware.util.io.ObjectWriter;

import java.util.Iterator;

/**
 * Wrapper for a simple iterator as SidesIterator
 * Intended misspelling.
 */
public class Proxyterator implements ObjectWriter.SidesIterator
{

    private final Iterator<?> underlying;

    public Proxyterator(Iterator<?> underlying)
    {
        if (underlying == null)
        {
            throw new NullPointerException("Underlying iterator may not be null");
        }
        this.underlying = underlying;
    }

    public boolean hasNext()
    {
        return underlying.hasNext();
    }

    public Object next()
    {
        return underlying.next();
    }
}
