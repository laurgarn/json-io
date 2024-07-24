package com.cedarsoftware.io;

import java.util.ArrayList;

import java.io.IOException;
import java.io.Writer;

public class BareOutputAutomaton extends OutputAutomaton {

	final Writer writer;

	final ArrayList<Byte> packedStack;
	int currentDepth;

	private State currentState;

	private String rollbackableKey;
	private Character rollbackableSep;

	public BareOutputAutomaton(Writer writer, boolean throwsOnBadMove) {
		super(throwsOnBadMove);

		this.writer = writer;
		this.currentDepth = 0;
		this.packedStack = new ArrayList<>();

		this.currentState = State.Void;
		this.rollbackableKey = null;
		this.rollbackableSep = null;
	}

	public State getCurrentState() {
		return currentState;
	}

	@Override
	protected boolean isStackEmpty()
	{
		return false;
	}

	@Override
	protected void program(Move move, State nextState, Character preSeparator, String str, State toSetOnPrevTop,
			boolean isToBeQuoted)
	{
		if (nextState != null)
			setCurrentState(nextState);

	}

	protected boolean fromVoid(Move move) {
		switch (move) {
			case OS:
				push(State.Void);
				doObjectStart(null);
				setCurrentState(State.WFK);
				return true;
			case AS:
				doArrayStart(':');
				setCurrentState(State.WV);
				push(State.Void);
				return true;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	protected boolean fromWaitingForKey(Move move, String key, boolean first) {
		switch (move) {
			case K:
				programString(key, first ? null : ',');
				setCurrentState(State.WFV);
				return true;
			case OE:
				boolean toRet = pop();
				if (toRet) doObjectEnd();
				return toRet;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	protected boolean fromWaitingForValue(Move move, String value, boolean first, boolean isToBeQuoted) {
		switch (move) {
			case V:
				doCommitLastKey();
				doString(value, ':', !isToBeQuoted);
				setCurrentState(State.WNK);
				return true;
			case OS:
				doCommitLastKey();
				doObjectStart(':');
				setCurrentState(State.WFK);
				push(State.WNK);
				return true;
			case AS:
				doCommitLastKey();
				doArrayStart(':');
				setCurrentState(State.WV);
				push(State.WNK);
				return true;
			case R:
				doRollback();
				setCurrentState(first ? State.WFK : State.WNK);
				return true;
			default:
				return returnFalseOrThrow("Move " + move + " not allowed from " + currentState);
		}
	}

	protected boolean fromWaitingInArray(Move move, String str, boolean isFirst, boolean isToBeQuoted) {
		switch (move) {
			case V:
				doString(str, isFirst ? null : ',', true);
				setCurrentState(State.GV);
				return true;
			case AE:
				boolean toRet = pop();
				doArrayEnd();
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

	public static int nbChunksToUse(int depth, int oneDeepBits, int largerChunksBits) {
		int bitsToUse = depth * oneDeepBits;
		int bitsOver = bitsToUse % largerChunksBits;
		return bitsToUse / largerChunksBits + (bitsOver == 0 ? 0 : 1);
	}

	private void doObjectStart(Character character) {
		try {
			if (character != null) {
				writer.write(character);
			}
			writer.write("{");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void doArrayStart(Character character) {
		try {
			if (character != null) {
				writer.write(character);
			}
			writer.write("[");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void programString(String key, Character character) {
		rollbackableKey = key;
		rollbackableSep = character;
	}

	private void doString(String key, Character c, boolean allowsNotQuoted) {
		try {
			if (c != null) {
				writer.write(c);
			}
		if (!needsQuote(key, allowsNotQuoted)) {
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

	private void doObjectEnd() {
		try {
			writer.write("}");
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

	protected boolean doRollback() {
		if (rollbackableKey == null && rollbackableSep == null)
			return false;

		rollbackableKey = null;
		rollbackableSep = null;
		return true;
	}

	@Override
	protected void mayFlush()
	{

	}

	private void doArrayEnd() {
		try {
			writer.write("]");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void push(State state) {
		pushPacked(state);
	}

	private boolean pop() {
		if (packedStack.isEmpty()) {
			return returnFalseOrThrow("State stack empty, can not pop");
		}

		popPacked();
		return true;
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

	void popPacked() {
		if (currentDepth <= 0) {
			// TODO return false or throw
			return;
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
	}
}
