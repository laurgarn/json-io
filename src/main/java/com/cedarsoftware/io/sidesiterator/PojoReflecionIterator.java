package com.cedarsoftware.io.sidesiterator;

import com.cedarsoftware.io.ObjectWriter;
import com.cedarsoftware.io.WriteOptions;
import com.cedarsoftware.io.reflect.Accessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class PojoReflecionIterator implements ObjectWriter.SidesIterator
{
    public static class FieldNContent
    {
        public final Accessor field;
        public final Object content;

        public FieldNContent(Accessor field, Object content)
        {
            this.field = field;
            this.content = content;
        }
    }

    private final Iterator<FieldNContent> underlying;

    private Accessor latestField;

    public PojoReflecionIterator(Object obj, WriteOptions writeOptions)
    {
        Collection<Accessor> accessors = writeOptions.getAccessorsForClass(obj.getClass());
		//ClassDescriptors ass = ClassDescriptors.instance().getDeepAccessors(obj.getClass());

        ArrayList<FieldNContent> traversableSubs = new ArrayList<>();
        for (Accessor accessor : accessors)
        {
            try
            {
                final Object o = accessor.retrieve(obj);
                traversableSubs.add(new PojoReflecionIterator.FieldNContent(accessor, o));
            }
            catch (Throwable ignored)
            {
            }
        }

        underlying = traversableSubs.iterator();
    }

    public boolean hasNext()
    {
        return underlying.hasNext();
    }

    public Object next()
    {
        FieldNContent pair = underlying.next();
        latestField = pair.field;
        return pair.content;
    }

    public String currentKey()
    {
        return latestField.getActualFieldName();
    }

    public Accessor currentField()
    {
        return latestField;
    }
}
