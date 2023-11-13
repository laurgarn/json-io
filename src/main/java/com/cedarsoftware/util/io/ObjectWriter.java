package com.cedarsoftware.util.io;

import com.cedarsoftware.util.io.sidesiterator.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectWriter
{

    /**
     * Common interface for iterating over "immediate" sub objects of an object,
     * with some context.
     */
    public interface SidesIterator extends Iterator<Object>
    {
        default Object currentContext()
        {
            return null;
        }

        default String currentKey()
        {
            return null;
        }
    }

    public interface Visitor
    {
        SidesIterator startsVisit(Object object, int depth, String key);

        default void visitNext(Object object, int depth, Object prevContext, Object nextContext)
        {
        }

        default void endVisit(Object object, int depth)
        {
        }
    }

    static class DepthStep
    {
        public final Object o;
        public Object freeContext;
        public SidesIterator it;

        private DepthStep(Object o)
        {
            this.o = o;
        }
    }

    private final RefsAccounter refsAccounter;
    private final Writer out;

    public ObjectWriter(Writer out,
                        boolean leastDeep)
    {
        if (out == null)
        {
            throw new NullPointerException("out writer may not be null");
        }

        this.out = out;

        refsAccounter = new RefsAccounter(leastDeep);
    }

    /**
     * @param item Object (root) to serialized to JSON String.
     * @return String of JSON format representing complete object graph rooted by item.
     * @see JsonWriter#objectToJson(Object, java.util.Map)
     */
    public static String objectToJson(Object item)
    {
        return objectToJson(item, null);
    }

    /**
     * Convert a Java Object to a JSON String.
     *
     * @param item         Object to convert to a JSON String.
     * @param optionalArgs (optional) Map of extra arguments indicating how dates are formatted,
     *                     what fields are written out (optional).  For Date parameters, use the public static
     *                     DATE_TIME key, and then use the ISO_DATE or ISO_DATE_TIME indicators.  Or you can specify
     *                     your own custom SimpleDateFormat String, or you can associate a SimpleDateFormat object,
     *                     in which case it will be used.  This setting is for both java.util.Date and java.sql.Date.
     *                     If the DATE_FORMAT key is not used, then dates will be formatted as longs.  This long can
     *                     be turned back into a date by using 'new Date(longValue)'.
     * @return String containing JSON representation of passed in object root.
     */
    public static String objectToJson(Object item, Map<String, Object> optionalArgs)
    {
        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            JsonWriter writer = new JsonWriter(stream, optionalArgs);
            writer.write(item);
            writer.close();
            return stream.toString(StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new JsonIoException("Unable to convert object to JSON", e);
        }
    }

    /**
     * Write the passed in Java object in JSON format.
     *
     * @param obj Object any Java Object or JsonObject.
     */
    public void write(Object obj)
    {
        driveIn(obj, (object, depth, key) -> refsAccounter.recordOneUse(object, depth) ? null : emptyterator());

        WriteOptions config = WriterContext.instance().getWriteOptions();

        JsonOutputAutomaton autom = new JsonOutputAutomaton(out, false, null);

        Visitor writingVisitor = new Visitor()
        {
            public SidesIterator startsVisit(Object object, int depth, String key)
            {
                if (object == null)
                {
                    return writesNull(key, autom, config);
                }
                else if (object instanceof JsonObject)
                {
                    // LGA probably to be handled by fallbackSequence
                    // symmetric support for writing Map of Maps representation back as equivalent JSON format.
                    JsonObject jObj = (JsonObject) object;
                    if (jObj.isArray())
                    {
//                        writeJsonObjectArray(jObj, showType);
                    }
                    else if (jObj.isCollection())
                    {
//                        writeJsonObjectCollection(jObj, showType);
                    }
                    else if (jObj.isMap())
                    {
//                        if (!writeJsonObjectMapWithStringKeys(jObj, showType))
//                        {
//                            writeJsonObjectMap(jObj, showType);
//                        }
                    }
                    else
                    {
//                        writeJsonObjectObject(jObj, showType);
                    }
                }
                else if (false)
                {
                    // delegation / plugins for primitive like
                }
                else if (MetaUtils.isLogicalPrimitive(object.getClass()))
                {
                    return writesPrimitive(obj, key, autom, config);
                }
                else
                {
                    Long codedId = refsAccounter.getCodedId(object, depth);
                    if (codedId != null && codedId > 0)
                    {
                        autom.emitObjectStart();
                        autom.emitKey(config.isUsingShortMetaKeys() ? "@r" : "@ref");
                        autom.emitValue(codedId.toString());
                        autom.emitObjectEnd();

                        return emptyterator();
                    }

                    if (codedId == null)
                    {
                        // code alignement
                    }
                    else if (codedId > 0)
                    {
                        // just ref
                    }
                    else
                    {
                        // drop id too
                    }
                }

                return emptyterator();
            }

            public void visitNext(Object object, int depth, Object prevContext, Object nextContext)
            {
                if (prevContext == nextContext)
                    return;
                if (prevContext != null)
                    autom.emitEnd();
                // according to next context, opens either { or [
            }

            public void endVisit(Object object, int depth)
            {
                if (null == object)
                {
                    return;
                }

                // delegation / plugins

                if (MetaUtils.isLogicalPrimitive(object.getClass()))
                {
                    return;
                }

                autom.emitEnd();
            }
        };
        driveIn(obj, writingVisitor);
    }

    private static SidesIterator writesNull(String key, JsonOutputAutomaton autom, WriteOptions config)
    {
        if (key == null)
        {
            autom.emitValue("null");
        }
        else if (!config.isSkippingNullFields())
        {
            autom.emitKey(key);
            autom.emitValue("null");
        }
        return emptyterator();
    }

    private static SidesIterator writesPrimitive(Object obj, String key, JsonOutputAutomaton autom, WriteOptions config)
    {
        assert obj != null;

        if (key != null) {
            autom.emitKey(key);
        }

        boolean allowsNanAndInfinity = config.isSkippingNullFields();

        boolean showType = true;
        if (config.isNeverShowingType())
        {
            showType = false;
        }

        if (obj instanceof Character)
        {
            autom.emitValue(String.valueOf(obj));
        }
//        else if (obj instanceof Long && config.isWritingLongsAsStrings())
//        {
//            if (showType)
//            {
//                out.write(config.isUsingShortMetaKeys() ? "{\"@t\":\"" : "{\"@type\":\"");
//                out.write(getSubstituteTypeName("long"));
//                out.write("\",\"value\":\"");
//                out.write(obj.toString());
//                out.write("\"}");
//            }
//            else
//            {
//                out.write('"');
//                out.write(obj.toString());
//                out.write('"');
//            }
//        }
        else if (!allowsNanAndInfinity && obj instanceof Double && (Double.isNaN((Double) obj) || Double.isInfinite((Double) obj)))
        {
            autom.emitValue("null");
        }
        else if (!allowsNanAndInfinity && obj instanceof Float && (Float.isNaN((Float) obj) || Float.isInfinite((Float) obj)))
        {
            autom.emitValue("null");
        }
        else
        {
            autom.emitValue(obj.toString());
        }

        return emptyterator();
    }

    public void flush()
    {
        try
        {
            out.flush();
        }
        catch (Exception ignored)
        {
        }
    }

    public void close()
    {
        try
        {
            out.close();
        }
        catch (Exception ignore)
        {
        }
    }

    private boolean tellIfDiff(Object one, Object two, String contextStr)
    {
        if (Objects.equals(one, two))
        {
            return false;
        }
        System.err.printf("Δ %s (%s) vs (%s) %n", contextStr, one, two);
        return true;
    }

    private void driveIn(Object object, Visitor visitor)
    {
        //IdentityHashMap<Object, Object> guard = new IdentityHashMap<>();

        ArrayList<DepthStep> explicitStack = new ArrayList<>();
        explicitStack.add(new DepthStep(object));

        while (!explicitStack.isEmpty())
        {
            DepthStep top = explicitStack.get(explicitStack.size() - 1);

            if (top.it == null)
            {
                Object theObj = top.o;
                String key = null;
                if (explicitStack.size() > 1) {
                    DepthStep almostTop = explicitStack.get(explicitStack.size() - 2);
                    key = almostTop.it.currentKey();
                }
                SidesIterator goAheadVisitor = visitor.startsVisit(theObj, explicitStack.size(), key);

                top.it = goAheadVisitor != null ? goAheadVisitor : fallbackSequence(theObj);
            }

            Object preContext = top.freeContext;// null at first time
            if (top.it.hasNext())
            {
                explicitStack.add(new DepthStep(top.it.next()));
                top.freeContext = top.it.currentContext();
                Object postContext = top.freeContext;
                visitor.visitNext(top.o, explicitStack.size(), preContext, postContext);
            }
            else
            {
                visitor.visitNext(top.o, explicitStack.size() + 1, preContext, null);
                DepthStep toQuit = explicitStack.remove(explicitStack.size() - 1);
                visitor.endVisit(toQuit.o, explicitStack.size() + 1);
            }
        }
    }

    private static SidesIterator emptyterator()
    {
        return Emptyterator.instance;
    }

    private SidesIterator fallbackSequence(Object obj)
    {
        if (obj == null)
        {
            return emptyterator();
        }

        final Class<?> clazz = obj.getClass();

        if (MetaUtils.isLogicalPrimitive(clazz))
        {
            return emptyterator();
        }

        if (clazz.isArray())
        {
            return new ArrayIterator(obj);
        }

        if (Map.class.isAssignableFrom(clazz))
        {   // logically walk maps, as opposed to following their internal structure.
            try
            {
                Map<?, ?> map = (Map<?, ?>) obj;
                boolean hasToUseParalleleSequences = map.keySet().stream().anyMatch(ObjectWriter::problematicAsKey);
                if (hasToUseParalleleSequences)
                {
                    return new KeysThenValuesMapIterator(map);
                }
                else
                {
                    return new StringKeyedMapIterator((Map<String, ?>) map);
                }
            }
            catch (UnsupportedOperationException e)
            {
                // Some kind of Map that does not support .entrySet() - some Maps throw UnsupportedOperation for
                // this API.  Do not attempt any further tracing of references.  Likely a ClassLoader field or
                // something unusual like that.
            }
        }

        if (obj instanceof Collection)
        {
            return new Proxyterator(((Collection<?>) obj).iterator());
        }

        return new PojoReflecionIterator(obj);
    }

    private static boolean problematicAsKey(Object k)
    {
        if (k == null)
        {
            return true;
        }

        if (k instanceof String)
        {
            String s = (String) k;
            return s.startsWith("@");
        }

        return true;
    }
}
