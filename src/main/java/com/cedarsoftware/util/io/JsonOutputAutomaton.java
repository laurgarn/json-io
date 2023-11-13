package com.cedarsoftware.util.io;

import java.util.ArrayList;

import java.io.IOException;
import java.io.Writer;

/**
 * Write Json incrementally.
 * Moves OS, OE, K, AS, AE, V. R
 *  Allowed from :
 *  0 : OS -> 1, AS -> 2
 *  1 : K -> 3, OE -> pop
 *  3 : V -> 5:, OS -> 1 + push(5), AS -> 2 + push(5), R -> 1
 *  5 : K -> 7+, OE -> pop
 *  7 : V -> 5:, OS -> 1 + push(5), AS -> 2 + push(5), R -> 5
 *  2 : V -> 4, AE -> pop, AS -> 2 + push(4)
 *  4 : V -> 4+ , AE -> pop, AS -> 2 + push(4)
 */
public class JsonOutputAutomaton {
	enum State {
		Void,
		WFK,
		WFV,
		WNK,
		WNV,
		WV,
		GV,
	}

	enum Move {
		OS("Object start"),
		K("Key"),
		V("Value"),
		R("Rollback"),
		OE("Object end"),
		AS("Array start"),
		AE("Array end");

		Move(String str) {
			this.str = str;
		}

		public String toString() {
			return str;
		}

		final String str;
	}

	final Writer writer;
	final boolean throwsOnBadMove;
	final String indentChunks;
	final ArrayList<State> easyStack;

	final ArrayList<Byte> packedStack;
	int currentDepth;

	private State currentState;

	private String rollbackableKey;
	private Character rollbackableSep;

	public JsonOutputAutomaton(Writer writer, boolean throwsOnBadMove, String indentChunks) {
		this.writer = writer;
		this.throwsOnBadMove = throwsOnBadMove;
		this.indentChunks = indentChunks;
		this.currentDepth = 0;
		this.packedStack = new ArrayList<>();
		this.easyStack = new ArrayList<>();

		this.currentState = State.Void;
		this.rollbackableKey = null;
		this.rollbackableSep = null;
	}

	State getCurrentState() {
		return currentState;
	}

	public boolean emitObjectStart() {
		return dispatch(Move.OS, null);
	}

	public boolean emitObjectEnd() {
		return dispatch(Move.OE, null);
	}

	public boolean emitKey(String key) {
		return dispatch(Move.K, key);
	}

	public boolean emitArrayStart() {
		return dispatch(Move.AS, null);
	}

	public boolean emitArrayEnd() {
		return dispatch(Move.AE, null);
	}

	public boolean emitEnd() {
		switch (currentState) {
			case Void:
				return returnFalseOrThrow("Nothing to close");
			case WFK:
			case WNK:
				return emitObjectEnd();
			case WV:
			case GV:
				return emitArrayEnd();
			default:
				return returnFalseOrThrow("Close");
		}
	}

	public boolean emitValue(String value) {
		return dispatch(Move.V, value);
	}

	public boolean rollbackLastKey() {
		return dispatch(Move.R, null);
	}

	private boolean dispatch(Move move, String str) {
		switch (currentState) {
			case Void:
				return fromVoid(move);
			case WFK:
			case WNK:
				return fromWaitingForKey(move, str, currentState == State.WFK);
			case WFV:
			case WNV:
				return fromWaitingForValue(move, str, currentState == State.WFV);
			case WV:
			case GV:
				return fromWaitingInArray(move, str, currentState == State.WV);
		}

		return false;
	}

	private boolean fromVoid(Move move) {
		switch (move) {
			case OS:
				push(State.Void);
				doObjectStart(null);
				setCurrentState(State.WFK);
				return true;
			case AS:
				push(State.Void);
				doArrayStart(':');
				setCurrentState(State.WV);
				return true;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	private boolean fromWaitingForKey(Move move, String key, boolean first) {
		switch (move) {
			case K:
				programString(key, first ? null : ',');
				setCurrentState(first ? State.WFV : State.WNV);
				return true;
			case OE:
				boolean toRet = pop();
				if (toRet) doObjectEnd(first);
				return toRet;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	private boolean fromWaitingForValue(Move move, String value, boolean first) {
		switch (move) {
			case V:
				doCommitLastKey();
				doString(value, ':', true);
				setCurrentState(State.WNK);
				return true;
			case OS:
				doCommitLastKey();
				push(State.WNK);
				doObjectStart(':');
				setCurrentState(State.WFK);
				return true;
			case AS:
				doCommitLastKey();
				push(State.WNK);
				doArrayStart(':');
				setCurrentState(State.WV);
				return true;
			case R:
				doRollback();
				setCurrentState(first ? State.WFK : State.WNK);
				return true;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	private boolean fromWaitingInArray(Move move, String str, boolean isFirst) {
		switch (move) {
			case V:
				doString(str, isFirst ? null : ',', true);
				setCurrentState(State.GV);
				return true;
			case AE:
				boolean toRet = pop();
				doArrayEnd(isFirst);
				return toRet;
			case OS:
				doObjectStart(isFirst ? null : ',');
				setCurrentState(State.WFK);
				push(State.GV);
				return true;
			case AS:
				doCommitLastKey();
				doArrayStart(isFirst ? null : ',');
				setCurrentState(State.WV);
				push(State.GV);
				return true;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	void setCurrentState(State nextState) {
		this.currentState = nextState;
	}

	public static int nbChunksToUsed(int depth, int oneDeepBits, int largerChunksBits) {
		int bitsToUse = depth * oneDeepBits;
		int bitsOver = bitsToUse % largerChunksBits;
		return bitsToUse / largerChunksBits + (bitsOver == 0 ? 0 : 1);
	}

	private void doObjectStart(Character character) {
		try {
			if (character != null) {
				writer.write(character);
				if (character == ',') {
					mayIndent();
				}
			}
			writer.write('{');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void doArrayStart(Character character) {
		try {
			if (character != null) {
				writer.write(character);
				if (character == ',') {
					mayIndent();
				}
			}
			writer.write('[');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void programString(String key, Character character) {
		rollbackableKey = key;
		rollbackableSep = character;
	}

	private void doString(String key, Character c, boolean allowsUnquoted) {
		try {
            if (c == null) {
                mayIndent();
            } else {
                writer.write(c);
                if (c == ',') {
                    mayIndent();
                }
			}

			if (allowsUnquoted && !needsQuote(key, true)) {
				writer.write(key);
			} else {
				String escaped = escapedUtf8String(key);
				writer.write('"');
                writer.write(escaped);
				writer.write('"');
            }
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean needsQuote(String s, boolean allowsQuoted) {
        final int len = s.length();
		if (len == 0)
			return true;
		char fc = s.charAt(0);
		switch (fc) {
			case 'n':
				return !s.equals("null");
			case 't':
				return !s.equals("true");
			case 'f':
				return !s.equals("false");
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return !isJsonNumber(s);
			case '"':
				return !allowsQuoted || len < 2 || s.charAt(len - 1) != '"'
						|| needsQuote(s.substring(1, len -2), false);
		}
		return true;
	}

	public static boolean isJsonNumber(String s) {
		if (s == null)
			return false;

		// 0:start 1:got_sign 2:first_zero 3:got_not_zero 4:got_dot 5:got_after_dot 6:got_exp 7:got_after_exp
		final int len = s.length();
		int state = 0;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '-':
					if (state == 0)
						state = 1;
					else if (state == 6)
						state = 7;
					else
						return false;
				case '0':
					if (state == 2)
						return false;
					if (state == 0)
						state = 2;
					break;
				case '.':
					if (state != 2 && state != 3)
						return false;
					state = 4;
					break;
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					if (state == 2)
						return false;
					if (state == 0 || state == 1)
						state = 3;
					else if (state == 4)
						state = 5;
					else if (state == 7)
						state = 8;
					break;
				case 'e':
				case 'E':
                    if (state != 3 && state != 5)
                        return false;
					state = 6;
					break;
                default:
					return false;
			}
		}

		switch (state) {
			case 0:
			case 1:
			case 4:
			case 6:
			case 7:
				return false;
	}
		return true;
	}

	/**
	 * Write out special characters "\b, \f, \t, \n, \r", as such, backslash as \\
	 * quote as \" and values less than an ASCII space (20hex) as "\\u00xx" format,
	 * characters in the range of ASCII space to a '~' as ASCII, and anything higher in UTF-8.
	 *
	 * @param s String to be written in UTF-8 format on the output stream.
	 * @throws IOException if an error occurs writing to the output stream.
	 */
    public static String escapedUtf8String(String s)
    {
        final int len = s.length();
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++)
        {
            char c = s.charAt(i);

            if (c < ' ')
            {    // Anything less than ASCII space, write either in \\u00xx form, or the special \t, \n, etc. form
                switch (c)
                {
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        sb.append(String.format("\\u%04X", (int) c));
                        break;
                }
            }
            else if (c == '\\' || c == '"')
            {
                sb.append('\\');
                sb.append(c);
            }
            else
            {   // Anything else - write in UTF-8 form (multi-byte encoded) (OutputStreamWriter is UTF-8)
                sb.append(c);
            }
        }

        return sb.toString();
    }

	private void doObjectEnd(boolean first) {
		try {
			if (!first) {
				mayIndent();
			}
			writer.write('}');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void doArrayEnd(boolean isFirst) {
		try {
			if (!isFirst) {
				mayIndent();
			}
			writer.write(']');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void doCommitLastKey() {
		if (rollbackableKey != null) {
			doString(rollbackableKey, rollbackableSep, false);
			rollbackableKey = null;
			rollbackableSep = null;
		}
	}

	private void doRollback() {
		rollbackableKey = null;
		rollbackableSep = null;
	}

	private void mayIndent() throws IOException {
		if (indentChunks == null)
			return;

		writer.write("\n");

		if (indentChunks.isEmpty())
			return;

		for (int i = currentDepth > 0 ? currentDepth : easyStack.size() ; i > 0 ; i--)
			writer.write(indentChunks);
	}

	private void push(State state) {
		pushPacked(state);
	}

	private boolean pop() {
		return popPacked();
	}

	private void pushEasy(State state) {
		easyStack.add(state);
	}

	private boolean popEasy() {
		if (easyStack.isEmpty()) {
			return returnFalseOrThrow("State stack empty, can not pop");
		}

		setCurrentState(easyStack.remove(easyStack.size() - 1));
		return true;
	}

	private boolean returnFalseOrThrow(String reason) {
		if (throwsOnBadMove) {
			throw new IllegalStateException(reason);
		}

		return false;
	}

	void pushPacked() {
		pushPacked(currentState);
	}

	void pushPacked(State state) {
		final int oneDeepBits = 3;
		final int largerChunksBits = 8;

		int stateBits = state.ordinal();

		int unlimitedShift = currentDepth * oneDeepBits;
		int inChunkShift = unlimitedShift % largerChunksBits;
		int lastBitOffset = (inChunkShift + oneDeepBits - 1) % largerChunksBits;

		if (inChunkShift > 0) {
			int lastIndex = packedStack.size() - 1;
			long chunkBits = packedStack.get(lastIndex);
			long mask = ((1 << oneDeepBits) - 1) << inChunkShift;
			chunkBits &= ~mask;
			chunkBits |= (stateBits << inChunkShift);
			byte byteBits1 = (byte) chunkBits;
			packedStack.set(lastIndex, byteBits1);
		}

		if (lastBitOffset < oneDeepBits) {
			int chunkBits = stateBits >> (oneDeepBits - lastBitOffset - 1);
			byte byteBits = (byte) chunkBits;
			packedStack.add(byteBits);
		}

		currentDepth += 1;
	}

	boolean popPacked() {
		if (currentDepth <= 0) {
			return returnFalseOrThrow("State stack empty, can not pop");
		}

		final int oneDeepBits = 3;
		final int largerChunksBits = 8;
		final long byteMask = (1L << 8) - 1;

		int nextDepth = currentDepth - 1;
		int unlimitedShift = nextDepth * oneDeepBits;
		int inChunkShift = unlimitedShift % largerChunksBits;
		int lastBitOffset = (inChunkShift + oneDeepBits - 1) % largerChunksBits;


		int lastIndex = packedStack.size() - 1;
		long bits = 0;

		if (lastBitOffset < oneDeepBits) {
			long upperBits = packedStack.remove(lastIndex);
			int nbBitsToUse = lastBitOffset + 1;
			upperBits &= (1L << nbBitsToUse) - 1;
			upperBits <<= (oneDeepBits - lastBitOffset - 1);
			bits |= upperBits;
			lastIndex -= 1;
		}

		if (inChunkShift > 0) {
			byte byteBits = packedStack.get(lastIndex);
			long lowerBits = byteBits & byteMask;
			lowerBits >>>= inChunkShift;
			int nbBitsToUse = lastBitOffset < oneDeepBits ? oneDeepBits - lastBitOffset - 1 : oneDeepBits;
			lowerBits &=	(1L << nbBitsToUse) - 1;
			bits |= lowerBits;
		}

		currentDepth = nextDepth;
		setCurrentState(State.values()[(int)bits]);

		return true;
	}
}
