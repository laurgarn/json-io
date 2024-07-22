package com.cedarsoftware.io.sidesiterator;

import com.cedarsoftware.io.ObjectWriter;

/**
 * A degenerated iterator, that has the interface but do not iterate.
 * Intended misspelling.
 */
public class Emptyterator implements ObjectWriter.SidesIterator
{

    public static Emptyterator instance = new Emptyterator();

    public boolean hasNext()
    {
        return false;
    }

    public Object next()
    {
        return null;
    }
}
