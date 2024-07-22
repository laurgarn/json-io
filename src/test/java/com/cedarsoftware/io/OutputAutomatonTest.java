package com.cedarsoftware.io;

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
		doIt(new OutputAutomatonWithFormatting.Config(null, false, 0), 348);
	}

    @Test
	public void indented() throws IOException {
		doIt(new OutputAutomatonWithFormatting.Config("  ", true, 0), 400);
	}

    @Test
	public void repacked() throws IOException {
		doIt(new OutputAutomatonWithFormatting.Config("  ", true, 80), 364);
	}

	private void doIt(OutputAutomatonWithFormatting.Config config, int expectedLength) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
		OutputAutomaton autom;
		if (config != null)
			autom = new OutputAutomatonWithFormatting(writer, false, config);
		else
			autom = new BareOutputAutomaton(writer, false);

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
		assertFalse(autom.emitEnd());
		assertFalse(autom.flush());

		writer.close();
		String got = stream.toString();
		System.out.printf("Long: %d%n", got.length());
		System.out.println(got);

		if (autom instanceof OutputAutomatonWithFormatting)
		{
			OutputAutomatonWithFormatting withStats = (OutputAutomatonWithFormatting) autom;
			assertTrue(withStats.getMaxQueueLength() < 14, "Max queue length " + withStats.getMaxQueueLength() + " should be lower than 10");
		}
		assertEquals(expectedLength, got.length());
	}
}
