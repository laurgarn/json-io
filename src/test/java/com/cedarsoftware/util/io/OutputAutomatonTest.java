package com.cedarsoftware.util.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputAutomatonTest {

    @Test
	public void tight() throws IOException {
		doIt(null, 306);
	}

    @Test
	public void flat() throws IOException {
		doIt(new OutputAutomaton.Config(null, false), 358);
	}

    @Test
	public void indented() throws IOException {
		doIt(new OutputAutomaton.Config("  ", true), 400);
	}

	private void doIt(OutputAutomaton.Config config, int expectedLength) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
		OutputAutomaton autom = new OutputAutomaton(writer, false, config);

		assertFalse(autom.emitKey("keyBeforeStart"));
		assertTrue(autom.emitObjectStart());
			assertTrue(autom.emitKey("firstKey"));
			assertTrue(autom.emitValue("firstValue"));
			assertTrue(autom.emitKey("secondKey"));
			assertTrue(autom.emitValue("secondValue"));
			assertTrue(autom.emitKey("thirdKey"));
			assertTrue(autom.emitObjectStart());
				assertFalse(autom.emitArrayStart());
				assertTrue(autom.emitKey("oneDeepKey"));
				assertFalse(autom.emitKey("oneDeepRefusedKey"));
				assertTrue(autom.emitValue("oneDeepValue"));
				assertTrue(autom.emitKey("rollbackableKey"));
				assertTrue(autom.rollbackLastKey());
			assertTrue(autom.emitObjectEnd());
			assertTrue(autom.emitKey("fourthKey"));
			assertTrue(autom.emitArrayStart());
			assertTrue(autom.emitArrayEnd());
			assertTrue(autom.emitKey("fifthKey"));
			assertTrue(autom.emitArrayStart());
				assertTrue(autom.emitValue("firstArrayValue"));
				assertTrue(autom.emitArrayStart());
				assertTrue(autom.emitArrayEnd());
				assertTrue(autom.emitValue("thirdArrayValue"));
				assertTrue(autom.emitObjectStart());
				assertTrue(autom.emitObjectEnd());
			assertTrue(autom.emitArrayEnd());
			assertTrue(autom.emitKey("sixthKey"));
			assertTrue(autom.emitArrayStart());
				assertTrue(autom.emitValue("oneArrayValue"));
			assertTrue(autom.emitArrayEnd());
			assertTrue(autom.emitKey("seventhKey"));
			assertTrue(autom.emitObjectStart());
				assertTrue(autom.emitKey("oneKey"));
				assertTrue(autom.emitValue("oneValue"));
				assertTrue(autom.emitKey("for null"));
				assertTrue(autom.emitValue("null"));
				assertTrue(autom.emitKey("for false"));
				assertTrue(autom.emitValue("false"));
				assertTrue(autom.emitKey("for true"));
				assertTrue(autom.emitValue("true"));
				assertTrue(autom.emitKey("for int"));
				assertTrue(autom.emitValue("2"));
				assertTrue(autom.emitKey("for float"));
				assertTrue(autom.emitValue("3.0e6"));
			assertTrue(autom.emitObjectEnd());
		assertTrue(autom.emitObjectEnd());
		assertFalse(autom.emitObjectEnd());
		autom.flush();
		writer.close();
		String got = stream.toString(StandardCharsets.UTF_8);
		System.out.printf("Long: %d%n", got.length());
		System.out.println(got);
		//assertEquals(config ? 400 : 306, got.length());
		assertEquals(expectedLength, got.length());
	}
}
