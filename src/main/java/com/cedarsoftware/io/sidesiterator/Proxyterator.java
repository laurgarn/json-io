package com.cedarsoftware.io.sidesiterator;

import com.cedarsoftware.io.ObjectWriter;

import java.util.Iterator;

/**
 * Wrapper for a simple iterator as SidesIterator
 * Intended misspelling.
 */
public class Proxyterator implements ObjectWriter.SidesIterator
{

    private final Iterator<?> underlying;

    private final boolean twoDeep;

    public Proxyterator(Iterator<?> underlying, boolean twoDeep)
    {
        this.twoDeep = twoDeep;
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

    public boolean isTwoDeep() {
        return twoDeep;
    }

}
