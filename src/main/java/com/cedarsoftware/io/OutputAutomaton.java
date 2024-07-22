package com.cedarsoftware.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.List;

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
public abstract class OutputAutomaton
{

    public enum State
    {
		Void,
		WFK,
		WFV,
		WNK,
		WNV,
		WV,
		GV,
	}

    protected enum Move
    {
		OS("Object start"),
		K("Key"),
		V("Value"),
		R("Rollback"),
		OE("Object end"),
		AS("Array start"),
		AE("Array end");

        Move(String str)
        {
			this.str = str;
		}

        public String toString()
        {
			return str;
		}

		final String str;
	}

    private final boolean throwsOnBadMove;

    public OutputAutomaton(boolean throwsOnBadMove)
		{
		this.throwsOnBadMove = throwsOnBadMove;
	}

    static Character getMoveChar(Move move)
    {
        switch (move)
        {
			case OS:
                return '{';
			case AS:
                return '[';
			case OE:
                return '}';
			case AE:
                return ']';
		}
		return null;
	}

    public static boolean needsQuote(String s, boolean allowsQuoted)
    {
        final int len = s.length();
		if (len == 0)
        {
			return true;
        }
		char fc = s.charAt(0);
        switch (fc)
        {
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
                return !OutputAutomaton.isJsonNumber(s);
			case '"':
				return !allowsQuoted || len < 2 || s.charAt(len - 1) != '"'
                        || OutputAutomaton.needsQuote(s.substring(1, len - 2), false);
		}
		return true;
	}

    public static boolean isJsonNumber(String s)
    {
		if (s == null)
        {
			return false;
        }

		// 0:start 1:got_sign 2:first_zero 3:got_not_zero 4:got_dot 5:got_after_dot 6:got_exp 7:got_after_exp
		final int len = s.length();
		int state = 0;
        for (int i = 0; i < len; i++)
        {
			char c = s.charAt(i);
            switch (c)
            {
				case '-':
					if (state == 0)
                    {
						state = 1;
                    }
					else if (state == 6)
                    {
						state = 7;
                    }
					else
                    {
						return false;
                    }
				case '0':
					if (state == 2)
                    {
						return false;
                    }
					if (state == 0)
                    {
						state = 2;
                    }
					else if (state == 4)
                    {
						state = 5;
                    }
					break;
				case '.':
					if (state != 2 && state != 3)
                    {
						return false;
                    }
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
                    {
						return false;
                    }
					if (state == 0 || state == 1)
                    {
						state = 3;
                    }
					else if (state == 4)
                    {
						state = 5;
                    }
					else if (state == 6 || state == 7)
                    {
						state = 8;
                    }
					break;
				case 'e':
				case 'E':
                    if (state != 3 && state != 5)
                    {
                        return false;
                    }
					state = 6;
					break;
                default:
					return false;
			}
		}

        switch (state)
        {
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

    public abstract State getCurrentState();

    public boolean emitObjectStart()
    {
        return dispatch(Move.OS, null);
		}

    public boolean emitObjectEnd()
    {
        return dispatch(Move.OE, null);
	}

    public boolean emitKey(String key)
    {
        return dispatch(Move.K, key);
    }

    public boolean emitArrayStart()
    {
        return dispatch(Move.AS, null);
    }

    public boolean emitArrayEnd()
    {
        return dispatch(Move.AE, null);
    }

    public boolean emitEnd()
    {
        switch (getCurrentState())
        {
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

    public boolean emitValue(String value)
    {
        return dispatch(Move.V, value);
    }

    public boolean rollbackLastKey()
    {
        return dispatch(Move.R, null);
		}

    private boolean dispatch(Move move, String str)
    {
        switch (getCurrentState())
        {
            case Void:
                return fromVoid(move);
            case WFK:
            case WNK:
                return fromWaitingForKey(move, str, getCurrentState() == State.WFK);
            case WFV:
            case WNV:
                return fromWaitingForValue(move, str, getCurrentState() == State.WFV);
            case WV:
            case GV:
                return fromWaitingInArray(move, str, getCurrentState() == State.WV);
        }

        return false;
    }

    protected abstract boolean isStackEmpty();

    protected boolean fromVoid(Move move)
    {
        switch (move)
        {
            case OS:
                program(move, State.WFK, null, null, null);
		return true;
            case AS:
                program(move, State.WV, null, null, null);
                return true;
            default:
                return returnFalseOrThrow("Move " + move + " not allowed from " + getCurrentState());
        }
	}

    protected boolean fromWaitingForKey(Move move, String key, boolean first)
    {
        switch (move)
        {
            case K:
                program(move, first ? State.WFV : State.WNV, first ? null : ',', key, null);
                return true;
            case OE:
                if (isStackEmpty())
                {
                    return false;
                }
                program(move, null, null, null, null);
                mayFlush();
                return true;
            case R:
                if (doRollback())
                {
                    return true;
                }
                // else fall through
            default:
                return returnFalseOrThrow("Move " + move + " not allowed from " + getCurrentState());
        }
	}

    protected boolean fromWaitingForValue(Move move, String value, boolean first)
    {
        switch (move)
        {
            case V:
                program(move, State.WNK, ':', value, null);
                mayFlush();
                return true;
            case OS:
                program(move, State.WFK, ':', null, State.WNK);
                return true;
            case AS:
                program(move, State.WV, ':', null, State.WNK);
                return true;
            case R:
                doRollback();
                return true;
            default:
                return returnFalseOrThrow("Move " + move + " not allowed from " + getCurrentState());
        }
		}

    protected boolean fromWaitingInArray(Move move, String str, boolean isFirst)
    {
        switch (move)
        {
            case V:
                program(move, State.GV, isFirst ? null : ',', str, null);
                mayFlush();
                return true;
            case AE:
                if (isStackEmpty())
                {
		return false;
	}
                program(move, null, null, null, null);
                mayFlush();
                return true;
            case OS:
                program(move, State.WFK, isFirst ? null : ',', null, State.GV);
                return true;
            case AS:
                program(move, State.WV, isFirst ? null : ',', null, State.GV);
                mayFlush();
                return true;
            default:
                return returnFalseOrThrow("Move " + move + " not allowed from " + getCurrentState());
        }
    }

    protected abstract void program(Move move, State nextState, Character preSeparator, String str, State toSetOnPrevTop);

    protected abstract boolean doRollback();

    protected abstract void mayFlush();

	public boolean flush() {
        return false;
    }

    protected boolean returnFalseOrThrow(String reason)
    {
        if (throwsOnBadMove)
        {
            throw new IllegalStateException(reason);
        }

        return false;
	}
}
