package com.cedarsoftware.util.io;

import com.cedarsoftware.util.io.sidesiterator.*;
import com.cedarsoftware.util.reflect.Accessor;

import java.io.Writer;
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

    public interface CustomWriter
    {
        default SidesIterator startWriteOn(Object objectToWrite, JsonOutputAutomaton automaton, WriteOptions options)
        {
            return Emptyterator.instance;
        }

        default void endWrite(Object objectToWrite) { }
    }

    interface Visitor
    {
        SidesIterator startsVisit(Object object, int depth, String key, Accessor accessor, Object context);

        default void visitNext(Object object, int depth, Object prevContext, Object nextContext)
        {
        }

        default void endVisit(Object object, int depth, SidesIterator it)
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

    private final WriteOptions config;

    public ObjectWriter(WriteOptions config)
    {
        if (config == null)
            config = WriterContext.instance().getWriteOptions();
        if (config == null)
            config = new WriteOptionsBuilder().build();
        this.config = config;
    }

    public void write(Object obj, Writer out)
    {
        final boolean leastDeep = true; // TODO from config
        RefsAccounter refsAccounter = new RefsAccounter(leastDeep);

        driveIn(obj, (object, depth, key, accessor, context) -> refsAccounter.recordOneUse(object, depth) ? null : emptyterator());

        String indentChunks = config.isPrettyPrint() ? "  " : null;
        JsonOutputAutomaton autom = new JsonOutputAutomaton(out, true, indentChunks);

        driveIn(obj, new OneGoWriter(config, refsAccounter, autom));
    }


    static class OneGoWriter implements Visitor {

        private final WriteOptions config;
        private final RefsAccounter refsAccounter;
        private final JsonOutputAutomaton autom;

        OneGoWriter(
                WriteOptions config,
                RefsAccounter refsAccounter,
                JsonOutputAutomaton autom
        ) {

            this.config = config;
            this.refsAccounter = refsAccounter;
            this.autom = autom;
        }

        public SidesIterator startsVisit(Object object, int depth, String key, Accessor accessor, Object context)
        {
            SidesIterator ret = onNull(object, key);
            if (ret != null) return ret;

            if (key != null)
            {
                // possible name rebinding thru accessor, as with Jackson annotations ?
                autom.emitKey(key);
            }

            ret = onJsonObject(object);
            if (ret != null) return ret;

            ret = onDelegatePrimitive(object, accessor);
            if (ret != null) return ret;

            ret = onPrimitive(object, accessor, context);
            if (ret != null) return ret;

            final Class<?> c = object.getClass();
            boolean showsType;
            if (config.isNeverShowingType()) {
                showsType = false;
            }
            else if (config.isAlwaysShowingType()) {
                showsType = true;
            }
            else {
                showsType = accessor == null || !Objects.equals(accessor.getType(), object.getClass());
            }

            Long codedId = refsAccounter.getCodedId(object, depth);
            boolean usesAKey = codedId != null || showsType;
            if (usesAKey)
            {
                autom.emitObjectStart();
            }

            if (codedId != null && codedId > 0)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@r" : "@ref");
                autom.emitValue(codedId.toString());
                //autom.emitObjectEnd(); // done by endVisit
                return emptyterator();
            }

            if (codedId != null)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@i" : "@id");
                autom.emitValue(Objects.toString(-codedId));
            }

            if (showsType)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@t" : "@type");
                String typeName = c.getName();
                String shortName = getSubstituteTypeNameIfExists(typeName, config);
                autom.emitValue(shortName != null ? shortName : typeName);
            }

            ret = onDelegateStructured(object, accessor, usesAKey);
            if (ret != null) return ret;

            ret = onArray(object, usesAKey);
            if (ret != null) return ret;

            ret = onMap(object, usesAKey);
            if (ret != null) return ret;

            ret = onEnumSet(object, usesAKey);
            if (ret != null) return ret;

            ret = onCollection(object, usesAKey);
            if (ret != null) return ret;

            return fieldByField(object, usesAKey);
        }

        SidesIterator onNull(Object object, String key)
        {
            if (object != null) {
                return null;
            }

            if (key != null && config.isSkippingNullFields()) {
                return emptyterator();
            }

            if (key != null)
            {
                autom.emitKey(key);
            }
            autom.emitValue("null");

            return emptyterator();
        }

        SidesIterator onJsonObject(Object object)
        {
            if (!(object instanceof JsonObject))
                return null;

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

            return null; // TODO
        }

        private SidesIterator onDelegatePrimitive(Object object, Accessor accessor)
        {
            return null;
        }

        private SidesIterator onDelegateStructured(Object object, Accessor accessor, boolean usesAKey)
        {
            return null;
        }

        SidesIterator onPrimitive(Object object, Accessor accessor, Object context)
        {
            if (!MetaUtils.isLogicalPrimitive(object.getClass()))
            {
                return null;
            }

            boolean allowsNanAndInfinity = config.isSkippingNullFields();

            boolean showType = true;
            if (config.isNeverShowingType())
            {
                showType = false;
            }

            if (object instanceof Character)
            {
                autom.emitValue(String.valueOf(object));
            }
            else if (object instanceof Long && config.isWritingLongsAsStrings())
            {
                if (showType)
                {
                    autom.emitObjectStart();
                    autom.emitKey(config.isUsingShortMetaKeys() ? "@t" : "@type");
                    autom.emitValue("long");
                    autom.emitKey("value");
                    autom.emitValue(object.toString());
                    autom.emitObjectEnd();
                }
                else
                {
                    autom.emitValue(object.toString());
                }
            }
            else if (!allowsNanAndInfinity && object instanceof Double && (Double.isNaN((Double) object) || Double.isInfinite((Double) object)))
            {
                autom.emitValue("null");
            }
            else if (!allowsNanAndInfinity && object instanceof Float && (Float.isNaN((Float) object) || Float.isInfinite((Float) object)))
            {
                autom.emitValue("null");
            }
            else
            {
                autom.emitValue(object.toString());
            }

            return emptyterator();
        }

        SidesIterator onArray(Object object, boolean usesAKey)
        {
            if (!object.getClass().isArray())
            {
                return null;
            }

            if (usesAKey)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@e" : "@items");
            }
            autom.emitArrayStart();

            return new ArrayIterator(object, usesAKey);
        }

        SidesIterator onMap(Object object, boolean usesAKey)
        {
            if (!Map.class.isAssignableFrom(object.getClass()))
            {
                return null;
            }

            try
            {
                Map<?, ?> map = (Map<?, ?>) object;
                boolean hasToUseParalleleSequences = config.isForcingMapFormatWithKeyArrays()
                    || map.keySet().stream().anyMatch(ObjectWriter::problematicAsKey);
                if (hasToUseParalleleSequences)
                {
                    KeysThenValuesMapIterator ret = new KeysThenValuesMapIterator(map);
                    if (!usesAKey)
                    {
                        autom.emitObjectStart();
                    }

                    return ret;
                }
                else
                {
                    StringKeyedMapIterator ret = new StringKeyedMapIterator((Map<String, ?>) map);
                    if (!usesAKey)
                    {
                        autom.emitObjectStart();
                    }

                    return ret;
                }
            }
            catch (UnsupportedOperationException e)
            {
                // Some kind of Map that does not support .entrySet() - some Maps throw UnsupportedOperation for
                // this API.  Do not attempt any further tracing of references.  Likely a ClassLoader field or
                // something unusual like that.
            }

            return emptyterator();
        }

        SidesIterator onEnumSet(Object object, boolean usesAKey)
        {
            if (!(object instanceof EnumSet))
            {
                return null;
            }

            autom.emitArrayStart();

            return new Proxyterator(((Collection<?>) object).iterator(), usesAKey);
        }

        SidesIterator onCollection(Object object, boolean usesAKey)
        {
            if (!(object instanceof Collection))
            {
                return null;
            }

            if (usesAKey)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@e" : "@items");
            }
            autom.emitArrayStart();

            return new Proxyterator(((Collection<?>) object).iterator(), usesAKey);
        }

        SidesIterator fieldByField(Object object, boolean usesAKey)
        {
            if (!usesAKey)
            {
                autom.emitObjectStart();
            }

            return new PojoReflecionIterator(object);
        }

        public void visitNext(Object object, int depth, Object prevContext, Object nextContext)
        {
            if (prevContext == nextContext)
                return;

            if (prevContext != null)
                autom.emitEnd();

            if (nextContext == KeysThenValuesMapIterator.KEY_CONTEXT)
            {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@k" : "@keys");
                autom.emitArrayStart();
            }

            if (nextContext == KeysThenValuesMapIterator.VALUE_CONTEXT) {
                autom.emitKey(config.isUsingShortMetaKeys() ? "@e" : "@items");
                autom.emitArrayStart();
            }

            // according to next context, opens either { or [
        }

        public void endVisit(Object object, int depth, SidesIterator it)
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

            boolean hasToEmitTwoEnds = false;
            if (it instanceof Proxyterator) {
                hasToEmitTwoEnds = ((Proxyterator) it).isTwoDeep();
            } else if (it instanceof ArrayIterator) {
                hasToEmitTwoEnds = ((ArrayIterator) it).isTwoDeep();
            }

            if (hasToEmitTwoEnds) {
                autom.emitEnd();
            }

            autom.emitEnd();
        }
    }

    static String getSubstituteTypeNameIfExists(String typeName, WriteOptions options)
    {
        if (options.getCustomTypeMap().isEmpty()) {
            return null;
        }

        return options.getCustomTypeMap().get(typeName);
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
                Accessor accessor = null;
                Object context = null;
                if (explicitStack.size() > 1) {
                    DepthStep almostTop = explicitStack.get(explicitStack.size() - 2);
                    SidesIterator parentIt = almostTop.it;
                    if (parentIt instanceof PojoReflecionIterator) {
                        PojoReflecionIterator pojoIt = (PojoReflecionIterator) parentIt;
                        accessor = pojoIt.currentField();
                    }
                    key = parentIt.currentKey();
                    context = parentIt.currentContext();
                }
                SidesIterator goAheadVisitor = visitor.startsVisit(theObj, explicitStack.size(), key, accessor, context);

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
                visitor.endVisit(toQuit.o, explicitStack.size() + 1, top.it);
            }
        }
    }

    private static SidesIterator emptyterator()
    {
        return Emptyterator.instance;
    }

    private static SidesIterator fallbackSequence(Object obj)
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
            return new ArrayIterator(obj, false);
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
            return new Proxyterator(((Collection<?>) obj).iterator(), false);
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
