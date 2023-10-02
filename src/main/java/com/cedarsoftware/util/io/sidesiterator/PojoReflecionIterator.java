package com.cedarsoftware.util.io.sidesiterator;

import com.cedarsoftware.util.io.ObjectWriter;
import com.cedarsoftware.util.reflect.Accessor;
import com.cedarsoftware.util.reflect.ClassDescriptors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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

    public PojoReflecionIterator(Object obj)
    {
        var cd = ClassDescriptors.instance().getClassDescriptor(obj.getClass());
        Map<String, Accessor> as = cd.getAccessors();

        ArrayList<FieldNContent> traversableSubs = new ArrayList<>();
        for (Accessor accessor : as.values())
        {
            try
            {
                final Object o = accessor.retrieve(obj);
                traversableSubs.add(new PojoReflecionIterator.FieldNContent(accessor, o));
            }
            catch (Exception ignored)
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
        return latestField.getName();
    }

    public Accessor currentField()
    {
        return latestField;
    }
}
