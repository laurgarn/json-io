package com.cedarsoftware.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.List;

import java.io.IOException;
import java.io.Writer;

public class OutputAutomatonWithFormatting extends OutputAutomaton
{

	public static class Config {
		final public String indentChunk;
		final private boolean noSpaces;
		final int lineMaxLength;


		public Config() {
			indentChunk = "  ";
			noSpaces = true;
			lineMaxLength = 96;
		}

		public Config(String indentChunk, boolean noSpaces, int lineMaxLength) {
			this.indentChunk = indentChunk;
			this.noSpaces = noSpaces;
			this.lineMaxLength = lineMaxLength;
		}

		public boolean spaceBefore(Character c) {
			if (noSpaces) return false;
			return c != ',' && c != '{' && c != '[';
		}

		public boolean spaceAfter(Character c) {
			if (lineMaxLength > 0 && c == ',') return true;
			if (noSpaces) return false;
			return c != '}' && c != ']';
		}
	}

	static class Queue<E> extends ArrayList<E> {
		public void removeRange(int fromIndex, int toIndex) {
			super.removeRange(fromIndex, toIndex);
		}
	}

	static class Stack<E> extends ArrayList<E> {
		public E peek() {
			if (isEmpty()) throw new EmptyStackException();
			return get(size() - 1);
		}
		public E pop() {
			if (isEmpty()) throw new EmptyStackException();
			int lastIndex = size() - 1;
			E ret = get(lastIndex);
			remove(lastIndex);
			return ret;
		}
		public boolean empty() {
			return isEmpty();
		}
	}

	static class StateForIndent {
		final State initialState;
		final StateForIndent parent;
		final int depth;

		private int minUsedCols;
		private int unsureUsedCols;

		State currentState;
		private int maxDepthUnder;
		int directSubsDoneCount;
		Boolean needsIndent;
		Unit firstUnit;

		StateForIndent(State state, StateForIndent parent) {
			this.initialState = state;
			this.currentState = state;
			this.parent = parent;
			this.depth = parent == null ? 0 : parent.depth + 1;
		}

		public String toString() {
			return "SFI{" +
				" init=" + initialState +
				", currentState=" + currentState +
				", directSubsDoneCount=" + directSubsDoneCount +
				", needsIndent=" + needsIndent +
				", minUsedCols=" + minUsedCols +
				", unsure=" + unsureUsedCols +
				", maxDepthUnder=" + maxDepthUnder +
				", firstUnit=" + firstUnit +
				'}';
		}

		int getMinUsedCols()
		{
			return minUsedCols;
		}

		void addMinUsedCols(int toAdd, boolean forSure, Config config) {
			if (!forSure) {
				this.unsureUsedCols += toAdd;
				return;
			}

			toAdd += this.unsureUsedCols;
			this.unsureUsedCols = 0;
			this.minUsedCols += toAdd;

			if (parent != null && parent.needsIndent != Boolean.TRUE) {
				parent.addMinUsedCols(toAdd, true, null);
			}
			if (config != null) {
				updateUp(null, config, true);
			}
		}

		void resetUnsure() {
			unsureUsedCols = 0;
		}

		private boolean updateUp(StateForIndent sub, Config config, boolean mayHaveMoreToCome) {
			if (needsIndent != null)
				return false;

			if (sub != null && sub.needsIndent == Boolean.TRUE) {
				this.needsIndent = Boolean.TRUE;
				parent.updateUp(this, config, mayHaveMoreToCome);
				return true;
			}

			if (sub == null && parent != null && !mayHaveMoreToCome) {
					parent.directSubsDoneCount += 1;
					if (parent.maxDepthUnder <= this.maxDepthUnder)
						parent.maxDepthUnder = this.maxDepthUnder + 1;
			}

			boolean localRet = false;
			if (config != null) {
				localRet = needsIndent(this, config);
				if (localRet || !mayHaveMoreToCome) {
					needsIndent = localRet;
					localRet = true;
				}
			}

			if (parent != null)
				localRet |= parent.updateUp(this, config, true);

			return localRet;
		}
	}

	static boolean needsIndent(StateForIndent stateForIndent, Config config) {
		if (stateForIndent == null || config == null)
			return false;
		if (stateForIndent.needsIndent == Boolean.TRUE)
			return true;
		if (stateForIndent.needsIndent == Boolean.FALSE)
			return false;

		return stateForIndent.minUsedCols > config.lineMaxLength;
	}

	//@Getter
	static class Unit {
		private final String    str;
		private final Character sep;
		private final Move      move;
		private final boolean   isToBeQuoted;

		private StateForIndent level;

		private int charCount;

		public String getStr() {
			return str;
		}

		public Character getSep() {
			return sep;
		}

		public Move getMove() {
			return move;
		}

		public int getCharCount() {
			return charCount;
		}

		public void setCharCount(int charCount) {
			this.charCount = charCount;
		}


		public Unit(String str, Character sep, Move move, boolean isToBeQuoted) {
			this.str = str;
			this.sep = sep;
			this.move = move;
			this.isToBeQuoted = isToBeQuoted;
		}

		public String toString() {
			return "Unit{" +
				"move=" + move.name() +
				", str='" + str + '\'' +
				", sep=" + sep +
				", charCount=" + charCount +
				", level=" + (level == null ? "?" : level.depth) +
				'}';
		}
	}

	private final Writer writer;
	private final Config config;

	final Stack<StateForIndent> easyStack;
	final Queue<Unit> queue;

	private int maxDepth;
	private int maxQueueLength;

	public OutputAutomatonWithFormatting(Writer writer, boolean throwsOnBadMove, Config config) {
		super(throwsOnBadMove);
		this.writer = writer;
		this.config = config;
		this.maxDepth = -1;
		this.easyStack = new Stack<>();
		this.queue = new Queue<>();
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public int getMaxQueueLength() {
		return maxQueueLength;
	}

	@Override
	public State getCurrentState() {
		if (isStackEmpty()) return State.Void;
		return easyStack.peek().currentState;
	}

	@Override
	protected boolean isStackEmpty()
	{
		return easyStack.isEmpty();
	}

	@Override
	protected void program(Move move, State nextState, Character preSeparator, String str, State toSetOnPrevTop,
			boolean isToBeQuoted) {
		Unit newUnit = new Unit(str, preSeparator, move, isToBeQuoted);
		queue.add(newUnit);
		if (queue.size() > maxQueueLength) {
			maxQueueLength = queue.size();
		}

		if (nextState != null && (toSetOnPrevTop != null || easyStack.empty())) {
			updateOnStart(newUnit, nextState, toSetOnPrevTop);
		} else if (nextState != null) {
			updateOnStr(newUnit, nextState);
		} else {
			updateOnEnd(newUnit);
		}

	}

	private void updateOnStart(Unit unit, State toPush, State toSetOnPrevTop) {
		// toPush in WNV, GV
		StateForIndent upper = easyStack.isEmpty() ? null : easyStack.peek();
		StateForIndent newElem = new StateForIndent(toPush, upper);
		easyStack.add(newElem);
		unit.level = newElem;
		newElem.firstUnit = unit;

		if (maxDepth < easyStack.size())
			maxDepth = easyStack.size();

		if (config != null) {
			char c = unit.move == Move.OS ? '{' : '[';

			if (upper == null) {
				assert toSetOnPrevTop == null;
			} else {
				upper.currentState = toSetOnPrevTop;
				if (unit.sep != null) {
					int toAddToUpper = 0;
					if (config.spaceBefore(unit.sep)) toAddToUpper += 1;
					toAddToUpper += 1;
					if (config.spaceAfter(unit.sep) || config.spaceBefore(c))
						toAddToUpper += 1;
					upper.addMinUsedCols(toAddToUpper, false, null);
				}
			}

			int toAddToNew = 1;
			if (config.spaceAfter(c)) toAddToNew += 1;
			newElem.addMinUsedCols(toAddToNew, false, null);
		}
	}

	private void updateOnStr(Unit unit, State nextState) {
		StateForIndent extState = easyStack.peek();
		unit.level = extState;
		extState.currentState = nextState;

		// sep in [null, ',', ':' ]
		if (extState.needsIndent != null) return;

		Character sep = unit.getSep();
		String str = unit.getStr();

		int toAdd = str.length() + 2;
		if (config != null && sep != null) {
			if (config.spaceBefore(sep)) toAdd += 1;
			toAdd += 1;
			if (config.spaceAfter(sep)) toAdd += 1;
		}

		if (unit.move == Move.V)
			extState.directSubsDoneCount += 1;

		extState.addMinUsedCols(toAdd, unit.move == Move.V, config);

		if (config != null & unit.move == Move.V) {
			resolveLevel(extState, true);
		}
	}

	private void updateOnEnd(Unit unit) {
		StateForIndent popped = easyStack.pop();
		unit.level = popped;
		if (!easyStack.isEmpty()) {
			StateForIndent top = easyStack.peek();
			top.currentState = top.initialState == State.WFK ? State.WNK : State.GV;
		}

		if (config == null)
			return;

		char c = popped.initialState == State.WFK ? '}' : ']';
		if (popped.needsIndent == null) {
			int toAddToPopped = 0;
			if (config.spaceBefore(c)) toAddToPopped += 1;
			toAddToPopped += 1;
			popped.addMinUsedCols(toAddToPopped, true, null);
		}

		if (!easyStack.isEmpty()) {
			StateForIndent stateToUpdate = easyStack.peek();
			if (config.spaceAfter(c)) stateToUpdate.addMinUsedCols(1, true, null);
		}

		popped.updateUp(null, config, false);
	}

	private void resolveLevel(StateForIndent extState, boolean partialEvaluation) {
		if (config.lineMaxLength >= 0) {
			if (extState.getMinUsedCols() > config.lineMaxLength) {
				StateForIndent upper = extState;
				while (upper != null && upper.needsIndent == null) {
					upper.needsIndent = Boolean.TRUE;
					upper = upper.parent;
				}
				extState.needsIndent = Boolean.TRUE;
			} else if (! partialEvaluation){
				extState.needsIndent = Boolean.FALSE;
			}
		}
	}

	private boolean flushQueue(boolean carefully) {
		final int len = queue.size();
		int nbDone = 0;
		try {
			while (nbDone < len) {
				Unit unit = queue.get(nbDone);

				if (config == null) {
					flushOneFlat(unit);
				} else if (carefully && unit.level.needsIndent == null) {
					break;
				} else if (squashWithNext(unit, nbDone + 1)) {
					nbDone += 1;
				} else {
					flushOneIndented(unit);
				}
				nbDone += 1;
			}

			queue.removeRange(0, nbDone);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		return nbDone > 0;
	}

	private boolean squashWithNext(Unit unit, int nextOffset) throws IOException {
		if (nextOffset >= queue.size()) {
			return false;
		}
		if (unit.move != Move.OS && unit.move != Move.AS) {
			return false;
		}

		Unit nextUnit = queue.get(nextOffset);
		String pairToWrite = null;
		if (unit.move == Move.OS && nextUnit.move == Move.OE) {
			pairToWrite = "{}";
		} else if (unit.move == Move.AS && nextUnit.move == Move.AE) {
			pairToWrite = "[]";
		}

		if (pairToWrite != null) {
			boolean wraps = config != null && config.indentChunk != null
				&& unit.level.parent != null
				&& unit.level.parent.needsIndent != Boolean.FALSE
				&& unit.level.parent.firstUnit.move == Move.AS;
			flushTheSep(unit, wraps);
			writer.write(pairToWrite);
			return true;
		}

		return false;
	}

	private void flushOneFlat(Unit unit) throws IOException {
		if (unit.sep != null)
			writer.write(unit.sep);
		if (unit.str != null) {
			flushStrPart(unit);
		} else if (unit.move == null) {
			// breakpoint spot
		} else {
			Character c = getMoveChar(unit.move);
			writer.write(c);
		}
	}

	private void flushTheSep(Unit unit, boolean wraps) throws IOException {
		if (unit.sep != null) {
			if (config.spaceBefore(unit.sep))
				writer.write(' ');
			writer.write(unit.sep);
		}

		if (wraps) {
			writer.write('\n');
			if (!config.indentChunk.isEmpty()) {
				int nbTodo = unit.level.depth;
				if (unit.move  == Move.K)
					nbTodo += 1;
				else if (unit.move == Move.V && unit.level.firstUnit.move == Move.AS)
					nbTodo += 1;
				writeIndentFromLeftSide(nbTodo);
			}
		} else if (unit.sep != null) {
			if (config.spaceAfter(unit.sep))
				writer.write(' ');
		}
	}

	private boolean needIndentBeforeMoveStr(Unit unit) {
		if (config.indentChunk == null)
			return false;

		switch (unit.move) {
			case K:
			case OE:
			case AE:
				return unit.level.needsIndent != Boolean.FALSE;
			case V:
				if (unit.level.needsIndent == Boolean.FALSE)
					return false;
				return unit.level.firstUnit.move == Move.AS;
			case OS:
			case AS:
				if (unit.level.needsIndent != Boolean.FALSE)
					return false;
				if (unit.level.parent.needsIndent != Boolean.TRUE)
					return false;
				return unit.level.parent.firstUnit.move == Move.AS;
			default:
				return false;
		}
	}

	private void flushOneIndented(Unit unit) throws IOException {
		flushTheSep(unit, needIndentBeforeMoveStr(unit));

		if (unit.str != null) {
			flushStrPart(unit);
		} else {
			Character c = getMoveChar(unit.move);
			writer.write(c);
		}
	}

	private void writeIndentFromLeftSide(int nbTodo) throws IOException {
		for (int i = nbTodo; i > 0; i--)
			writer.write(config.indentChunk);
	}

	private void flushStrPart(Unit unit) throws IOException {
		if (!unit.isToBeQuoted && !needsQuote(unit.str, unit.move == Move.V)) {
			writer.write(unit.str);
		} else {
			String escaped = escapedUtf8String(unit.str);
			writer.write('"');
			writer.write(escaped);
			writer.write('"');
		}
	}

	@Override
	protected boolean doRollback() {
		if (!queue.isEmpty()) {
			return rollbackIfSuffix(Arrays.asList(Move.K))
				|| rollbackIfSuffix(Arrays.asList(Move.K, Move.OS))
				|| rollbackIfSuffix(Arrays.asList(Move.K, Move.AS));
		}

		return false;
	}

	private boolean rollbackIfSuffix(List<Move> moves) {
		int qlen = queue.size();
		int plen = moves.size();
		if (qlen < plen)
			return false;

		Unit prevLastUnit = queue.get(qlen - 1);
		for (int i = 0; i < plen; i++)
			if (queue.get(qlen - plen + i).move != moves.get(i))
				return false;

		for (int i = 0; i < plen; i++)
			queue.remove(qlen - 1 - i);

		if (prevLastUnit.move != Move.K) {
			easyStack.pop();
		}

		StateForIndent currentTop = easyStack.peek();
		currentTop.resetUnsure();

		if (currentTop.currentState == State.WFV) {
			currentTop.currentState = State.WFK;
		} else if (currentTop.currentState == State.WNV) {
			currentTop.currentState = State.WNK;
		} else {
			assert false;
		}

		return true;
	}

	@Override
	protected void mayFlush() {
		flushQueue(true);
	}

	@Override
	public boolean flush() {
		return flushQueue(false);
	}
}
