package com.cedarsoftware.util.io;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputAutomatonTest {

	public void nearlyByHand() throws IOException {
		doIt(true);
	}

	private void doIt(boolean withIdentation) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		var writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
		OutputAutomaton autom = new OutputAutomaton(writer, false, withIdentation ? "  " : null);

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
			assertTrue(autom.emitObjectEnd());
		assertTrue(autom.emitObjectEnd());
		assertFalse(autom.emitObjectEnd());
		writer.close();
		String got = stream.toString(StandardCharsets.UTF_8);
		System.out.printf("Long: %d%n", got.length());
		System.out.println(got);
		assertEquals(withIdentation ? 295 : 226, got.length());
	}
}
