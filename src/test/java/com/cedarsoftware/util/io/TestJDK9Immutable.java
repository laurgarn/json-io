package com.cedarsoftware.util.io;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestJDK9Immutable {

    static class Pair<K, V> {
        K k;
        V v;
        Pair(K k, V v) {
            this.k = k;
            this.v = v;
        }
    }

    static class Rec {
        final String s;
        final int i;
        Rec(String s, int i) {
            this.s = s;
            this.i = i;
        }

        Rec       link;
        List<Rec> ilinks;
        List<Rec> mlinks;

        Map<String, Rec> smap;
        Map<Pair<Integer, Character>, String> pmap;

		public String toString() {
			return "Rec{" +
				"s='" + s + '\'' +
				", i=" + i +
				'}';
		}
    }

    @Test
    public void testCopyOfListOf() {
        final Object o = new ArrayList<>(List.of());

        String json = JsonWriter.objectToJson(o);
        List<?> es = (List<?>) JsonReader.jsonToJava(json);

        assertEquals(0, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testListOf() {
        final Object o = List.of();

        String json = JsonWriter.objectToJson(o);
        List<?> es = (List<?>) JsonReader.jsonToJava(json);

        assertEquals(0, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testListOfOne() {
        final Object o = List.of("One");

        String json = JsonWriter.objectToJson(o);
        List<?> es = (List<?>) JsonReader.jsonToJava(json);

        assertEquals(1, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testListOfTwo() {
        final Object o = List.of("One", "Two");

        String json = JsonWriter.objectToJson(o);
        List<?> es = (List<?>) JsonReader.jsonToJava(json);

        assertEquals(2, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testListOfThree() {
        final Object o = List.of("One", "Two", "Three");

        String json = JsonWriter.objectToJson(o);
        List<?> es = (List<?>) JsonReader.jsonToJava(json);

        assertEquals(3, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testSetOf() {
        final Object o = Set.of();

        String json = JsonWriter.objectToJson(o);
        Set<?> es = (Set<?>) JsonReader.jsonToJava(json);

        assertEquals(0, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testSetOfOne() {
        final Object o = Set.of("One");

        String json = JsonWriter.objectToJson(o);
        Set<?> es = (Set<?>) JsonReader.jsonToJava(json);

        assertEquals(1, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testSetOfTwo() {
        final Object o = Set.of("One", "Two");

        String json = JsonWriter.objectToJson(o);
        Set<?> es = (Set<?>) JsonReader.jsonToJava(json);

        assertEquals(2, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testSetOfThree() {
        final Object o = Set.of("One", "Two", "Three");

        String json = JsonWriter.objectToJson(o);
        Set<?> es = (Set<?>) JsonReader.jsonToJava(json);

        assertEquals(3, es.size());
        assertEquals(o.getClass(), es.getClass());
    }

    @Test
    public void testListOfThreeRecsMutableBoth() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        rec1.link = rec2;
        rec2.link = rec1;
        rec1.mlinks = new ArrayList<>(List.of());
        rec2.mlinks = new ArrayList<>(List.of(rec1));
        List<Rec> ol = new ArrayList<>(List.of(rec1, rec2, rec1));

        String json = JsonWriter.objectToJson(ol);
        List<Rec> recs = (List<Rec>) JsonReader.jsonToJava(json);

        assertEquals(ol.getClass(), recs.getClass());
        assertEquals(ol.size(), recs.size());

        assertEquals(ol.get(0).s, recs.get(0).s);
        assertEquals(ol.get(0).i, recs.get(0).i);
        assertEquals(recs.get(1), recs.get(0).link);
        assertEquals(ol.get(1).s, recs.get(1).s);
        assertEquals(ol.get(1).i, recs.get(1).i);
        assertEquals(recs.get(0), recs.get(1).link);

        assertEquals(recs.get(0), recs.get(2));

        assertEquals(ol.get(0).mlinks.size(), recs.get(0).mlinks.size());
        assertEquals(ol.get(0).mlinks.getClass(), recs.get(0).mlinks.getClass());
        assertEquals(ol.get(1).mlinks.size(), recs.get(1).mlinks.size());
        assertEquals(ol.get(1).mlinks.getClass(), recs.get(1).mlinks.getClass());
    }

    @Test
    public void testListOfThreeRecsMutableInside() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        rec1.link = rec2;
        rec2.link = rec1;
        rec1.mlinks = new ArrayList<>(List.of());
        rec2.mlinks = new ArrayList<>(List.of(rec1));
        List<Rec> ol = List.of(rec1, rec2, rec1);

        String json = JsonWriter.objectToJson(ol);
        List<Rec> recs = (List<Rec>) JsonReader.jsonToJava(json);

        assertEquals(ol.getClass(), recs.getClass());
        assertEquals(ol.size(), recs.size());

        assertEquals(ol.get(0).s, recs.get(0).s);
        assertEquals(ol.get(0).i, recs.get(0).i);
        assertEquals(recs.get(1), recs.get(0).link);
        assertEquals(ol.get(1).s, recs.get(1).s);
        assertEquals(ol.get(1).i, recs.get(1).i);
        assertEquals(recs.get(0), recs.get(1).link);

        assertEquals(recs.get(0), recs.get(2));

        assertEquals(ol.get(0).mlinks.size(), recs.get(0).mlinks.size());
        assertEquals(ol.get(0).mlinks.getClass(), recs.get(0).mlinks.getClass());
        assertEquals(ol.get(1).mlinks.size(), recs.get(1).mlinks.size());
        assertEquals(ol.get(1).mlinks.getClass(), recs.get(1).mlinks.getClass());
    }

    @Test
    public void testListOfThreeRecsBoth() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        rec1.link = rec2;
        rec2.link = rec1;
        rec1.ilinks = List.of(rec2);
        rec2.ilinks = List.of();
        rec1.mlinks = new ArrayList<>(List.of());
        rec2.mlinks = new ArrayList<>(List.of(rec1));
        List<Rec> ol = List.of(rec1, rec2, rec1);

        String json = JsonWriter.objectToJson(ol);
        Object es = (List) JsonReader.jsonToJava(json);

        assertEquals(((Object) ol).getClass(), es.getClass());

		List<Rec> recs = (List<Rec>) es;
        assertEquals(ol.size(), recs.size());

        assertEquals(ol.get(0).s, recs.get(0).s);
        assertEquals(ol.get(0).i, recs.get(0).i);
        assertEquals(recs.get(1), recs.get(0).link);
        assertEquals(ol.get(1).s, recs.get(1).s);
        assertEquals(ol.get(1).i, recs.get(1).i);
        assertEquals(recs.get(0), recs.get(1).link);

        assertEquals(recs.get(0), recs.get(2));

        assertEquals(ol.get(0).mlinks.size(), recs.get(0).mlinks.size());
        assertEquals(ol.get(0).mlinks.getClass(), recs.get(0).mlinks.getClass());
        assertEquals(ol.get(1).mlinks.size(), recs.get(1).mlinks.size());
        assertEquals(ol.get(1).mlinks.getClass(), recs.get(1).mlinks.getClass());

        assertEquals(ol.get(0).ilinks.getClass(), recs.get(0).ilinks.getClass());
        assertEquals(ol.get(0).ilinks.size(), recs.get(0).ilinks.size());
        assertEquals(ol.get(1).ilinks.getClass(), recs.get(1).ilinks.getClass());
        assertEquals(ol.get(1).ilinks.size(), recs.get(1).ilinks.size());
    }

    @Test
    public void testListOfThreeRecsImmutableOnly() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        rec1.ilinks = List.of(rec2, rec1);
        rec2.ilinks = List.of();
        List<Rec> ol = List.of(rec1, rec2, rec1);

        rec1.smap = Map.of();

        String json = JsonWriter.objectToJson(ol);
        Object es = JsonReader.jsonToJava(json);

        assertEquals(((Object) ol).getClass(), es.getClass());

        List<Rec> recs = (List<Rec>) es;
        assertEquals(ol.size(), recs.size());

        assertEquals(ol.get(0).s, recs.get(0).s);
        assertEquals(ol.get(0).i, recs.get(0).i);
        assertEquals(ol.get(1).s, recs.get(1).s);
        assertEquals(ol.get(1).i, recs.get(1).i);

        assertEquals(recs.get(0), recs.get(2));

        assertEquals(ol.get(0).ilinks.getClass(), recs.get(0).ilinks.getClass());
        assertEquals(ol.get(0).ilinks.size(), recs.get(0).ilinks.size());
        assertEquals(ol.get(1).ilinks.getClass(), recs.get(1).ilinks.getClass());
        assertEquals(ol.get(1).ilinks.size(), recs.get(1).ilinks.size());
    }

    @Test
    public void testMapListOf() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        //rec1.link = rec2;
        //rec2.link = rec1;
        rec1.ilinks = List.of(rec2, rec1);
        rec2.ilinks = List.of();
		//rec2.map = new HashMap<>(Map.of("Zwei", rec2));
		rec2.smap = Map.of("Zwei", rec2);
        List<Rec> ol = List.of(rec1, rec2, rec1);

        String json = JsonWriter.objectToJson(ol);
        List<Rec> recs = (List<Rec>) JsonReader.jsonToJava(json);

        assertEquals(((Object) ol).getClass(), recs.getClass());

        assertEquals(ol.size(), recs.size());

        assertEquals(ol.get(0).s, recs.get(0).s);
        assertEquals(ol.get(0).i, recs.get(0).i);
        //assertEquals(recs.get(1), recs.get(0).link);
        assertEquals(ol.get(1).s, recs.get(1).s);
        assertEquals(ol.get(1).i, recs.get(1).i);
        //assertEquals(recs.get(0), recs.get(1).link);

        assertEquals(recs.get(0), recs.get(2));

        assertEquals(ol.get(0).ilinks.getClass(), recs.get(0).ilinks.getClass());
        assertEquals(ol.get(0).ilinks.size(), recs.get(0).ilinks.size());
        assertEquals(ol.get(1).ilinks.getClass(), recs.get(1).ilinks.getClass());
        assertEquals(ol.get(1).ilinks.size(), recs.get(1).ilinks.size());

		assertEquals(ol.get(1).smap.size(), recs.get(1).smap.size());
		assertEquals(ol.get(1).smap.getClass(), recs.get(1).smap.getClass());

    }

	@Test
	public void fromTwistedJson() {
		String a = "{ '@type':'java.util.ImmutableCollections$ListN', '@items':[ { '@id':1, '@type':'com.cedarsoftware.util.io.TestJDK9Immutable$Rec', 's':'OneOrThree', 'i':0, 'link':null, 'ilinks':{ '@type':'java.util.ImmutableCollections$List12', '@items':[ { '@ref':2 }, { '@ref':1 } ] }, 'mlinks':null, 'map':null }, { '@id':2, '@type':'com.cedarsoftware.util.io.TestJDK9Immutable$Rec', 's':'Two', 'i':2, 'link':null, 'ilinks':{ '@type':'java.util.ImmutableCollections$ListN' }, 'mlinks':null, 'map':{ '@type':'java.util.ImmutableCollections$Map1', 'Zwei':{ '@ref':1 } } }, { '@ref':1 } ] }";
		Object gotten = JsonReader.jsonToJava(a.replace("'", "\""));

		Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);

		assertNotNull(gotten);
		List<Rec> recs = (List<Rec>) gotten;

		assertEquals(List.of(rec1, rec2, rec1).getClass(), recs.getClass());
		assertEquals(List.of(rec1, rec2).getClass(), recs.get(0).ilinks.getClass());

	}

	@Test
	public void paf() {
        Rec rec1 = new Rec("OneOrThree", 0);
        Rec rec2 = new Rec("Two", 2);
        //rec1.link = rec2;
        //rec2.link = rec1;
        rec1.ilinks = List.of(rec2, rec1);
        rec2.ilinks = List.of();
		rec2.pmap = new HashMap<>(Map.of(new Pair<>(1, 'a'), "start", new Pair<>(5, 'b'), "end"));
		//rec2.map = new HashMap<>(Map.of("Zwei", rec2));
		rec2.smap = Map.of("Zwei", rec2);
        List<Rec> ol = List.of(rec1, rec2, rec1);

        CharArrayWriter fw = new CharArrayWriter(1000);
        ObjectWriter ow = new ObjectWriter(null);
        ow.write(ol, fw);
        System.out.println(fw);
	}
}
