/**
 * 
 */
package com.crm.util;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;

/**
 * @author ThangPV
 * 
 */
public class StringUtil
{
	public static final char[]	_HEX_DIGITS		=
												{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String[]		_emptyString	= new String[0];

	public static String add(String s, String add)
	{
		return add(s, add, StringPool.COMMA);
	}

	public static String add(String s, String add, String delimiter)
	{
		return add(s, add, delimiter, false);
	}

	public static String add(
			String s, String add, String delimiter, boolean allowDuplicates)
	{

		if ((add == null) || (delimiter == null))
		{
			return null;
		}

		if (s == null)
		{
			s = StringPool.BLANK;
		}

		if (allowDuplicates || !contains(s, add, delimiter))
		{
			StringBuffer sb = new StringBuffer();

			sb.append(s);

			if (Validator.isNull(s) || s.endsWith(delimiter))
			{
				sb.append(add);
				sb.append(delimiter);
			}
			else
			{
				sb.append(delimiter);
				sb.append(add);
				sb.append(delimiter);
			}

			s = sb.toString();
		}

		return s;
	}

	public static String appendParentheticalSuffix(String s, int suffix)
	{
		if (Pattern.matches(".* \\(" + String.valueOf(suffix - 1) + "\\)", s))
		{
			int pos = s.lastIndexOf(" (");

			s = s.substring(0, pos);
		}

		return appendParentheticalSuffix(s, String.valueOf(suffix));
	}

	public static String appendParentheticalSuffix(String s, String suffix)
	{
		StringBuffer sb = new StringBuffer(5);

		sb.append(s);
		sb.append(StringPool.SPACE);
		sb.append(StringPool.OPEN_PARENTHESIS);
		sb.append(suffix);
		sb.append(StringPool.CLOSE_PARENTHESIS);

		return sb.toString();
	}

	public static String bytesToHexString(byte[] bytes)
	{
		StringBuffer sb = new StringBuffer(bytes.length * 2);

		for (byte b : bytes)
		{
			String hex = Integer.toHexString(
					0x0100 + (b & 0x00FF)).substring(1);

			if (hex.length() < 2)
			{
				sb.append("0");
			}

			sb.append(hex);
		}

		return sb.toString();
	}

	public static boolean contains(String s, String text)
	{
		return contains(s, text, StringPool.COMMA);
	}

	public static boolean contains(String s, String text, String delimiter)
	{
		if ((s == null) || (text == null) || (delimiter == null))
		{
			return false;
		}

		if (!s.endsWith(delimiter))
		{
			s = s.concat(delimiter);
		}

		String dtd = delimiter.concat(text).concat(delimiter);

		int pos = s.indexOf(dtd);

		if (pos == -1)
		{
			String td = text.concat(delimiter);

			if (s.startsWith(td))
			{
				return true;
			}

			return false;
		}

		return true;
	}

	public static int count(String s, String text)
	{
		if ((s == null) || (text == null))
		{
			return 0;
		}

		int count = 0;

		int pos = s.indexOf(text);

		while (pos != -1)
		{
			pos = s.indexOf(text, pos + text.length());

			count++;
		}

		return count;
	}

	public static boolean endsWith(String s, char end)
	{
		return endsWith(s, (new Character(end)).toString());
	}

	public static boolean endsWith(String s, String end)
	{
		if ((s == null) || (end == null))
		{
			return false;
		}

		if (end.length() > s.length())
		{
			return false;
		}

		String temp = s.substring(s.length() - end.length(), s.length());

		if (temp.equalsIgnoreCase(end))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static String extract(String s, char[] chars)
	{
		if (s == null)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer();

		for (char c1 : s.toCharArray())
		{
			for (char c2 : chars)
			{
				if (c1 == c2)
				{
					sb.append(c1);

					break;
				}
			}
		}

		return sb.toString();
	}

	public static String extractChars(String s)
	{
		if (s == null)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer();

		char[] chars = s.toCharArray();

		for (char c : chars)
		{
			if (Validator.isChar(c))
			{
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static String extractDigits(String s)
	{
		if (s == null)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer();

		char[] chars = s.toCharArray();

		for (char c : chars)
		{
			if (Validator.isDigit(c))
			{
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static String extractFirst(String s, char delimiter)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			int index = s.indexOf(delimiter);

			if (index < 0)
			{
				return null;
			}
			else
			{
				return s.substring(0, index);
			}
		}
	}

	public static String extractFirst(String s, String delimiter)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			int index = s.indexOf(delimiter);

			if (index < 0)
			{
				return null;
			}
			else
			{
				return s.substring(0, index);
			}
		}
	}

	public static String extractLast(String s, char delimiter)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			int index = s.lastIndexOf(delimiter);

			if (index < 0)
			{
				return null;
			}
			else
			{
				return s.substring(index + 1);
			}
		}
	}

	public static String extractLast(String s, String delimiter)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			int index = s.lastIndexOf(delimiter);

			if (index < 0)
			{
				return null;
			}
			else
			{
				return s.substring(index + delimiter.length());
			}
		}
	}

	public static String insert(String s, String insert, int offset)
	{
		if (s == null)
		{
			return null;
		}

		if (insert == null)
		{
			return s;
		}

		if (offset > s.length())
		{
			return s.concat(insert);
		}
		else
		{
			String prefix = s.substring(0, offset);
			String postfix = s.substring(offset);

			return prefix.concat(insert).concat(postfix);
		}
	}

	public static String lowerCase(String s)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			return s.toLowerCase();
		}
	}

	public static boolean matches(String s, String pattern)
	{
		String[] array = pattern.split("\\*");

		for (String element : array)
		{
			int pos = s.indexOf(element);

			if (pos == -1)
			{
				return false;
			}

			s = s.substring(pos + element.length());
		}

		return true;
	}

	public static boolean matchesIgnoreCase(String s, String pattern)
	{
		return matches(lowerCase(s), lowerCase(pattern));
	}

	public static String merge(boolean[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(boolean[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(Collection<?> col)
	{
		return merge(col, StringPool.COMMA);
	}

	public static String merge(Collection<?> col, String delimiter)
	{
		if (col == null)
		{
			return null;
		}

		return merge(col.toArray(new Object[col.size()]), delimiter);
	}

	public static String merge(char[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(char[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(double[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(double[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(float[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(float[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(int[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(int[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(long[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(long[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(Object[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(Object[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String merge(short[] array)
	{
		return merge(array, StringPool.COMMA);
	}

	public static String merge(short[] array, String delimiter)
	{
		if (array == null)
		{
			return null;
		}

		if (array.length == 0)
		{
			return StringPool.BLANK;
		}

		StringBuffer sb = new StringBuffer(2 * array.length - 1);

		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.valueOf(array[i]).trim());

			if ((i + 1) != array.length)
			{
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static String quote(String s)
	{
		return quote(s, CharPool.APOSTROPHE);
	}

	public static String quote(String s, char quote)
	{
		if (s == null)
		{
			return null;
		}

		return quote(s, String.valueOf(quote));
	}

	public static String quote(String s, String quote)
	{
		if (s == null)
		{
			return null;
		}

		return quote.concat(s).concat(quote);
	}

	public static String remove(String s, String remove)
	{
		return remove(s, remove, StringPool.COMMA);
	}

	public static String remove(String s, String remove, String delimiter)
	{
		if ((s == null) || (remove == null) || (delimiter == null))
		{
			return null;
		}

		if (Validator.isNotNull(s) && !s.endsWith(delimiter))
		{
			s += delimiter;
		}

		String drd = delimiter.concat(remove).concat(delimiter);

		String rd = remove.concat(delimiter);

		while (contains(s, remove, delimiter))
		{
			int pos = s.indexOf(drd);

			if (pos == -1)
			{
				if (s.startsWith(rd))
				{
					int x = remove.length() + delimiter.length();
					int y = s.length();

					s = s.substring(x, y);
				}
			}
			else
			{
				int x = pos + remove.length() + delimiter.length();
				int y = s.length();

				String temp = s.substring(0, pos);

				s = temp.concat(s.substring(x, y));
			}
		}

		return s;
	}

	public static String replace(String s, char oldSub, char newSub)
	{
		if (s == null)
		{
			return null;
		}

		return s.replace(oldSub, newSub);
	}

	public static String replace(String s, char oldSub, String newSub)
	{
		if ((s == null) || (newSub == null))
		{
			return null;
		}

		// The number 5 is arbitrary and is used as extra padding to reduce
		// buffer expansion

		StringBuffer sb = new StringBuffer(s.length() + 5 * newSub.length());

		char[] chars = s.toCharArray();

		for (char c : chars)
		{
			if (c == oldSub)
			{
				sb.append(newSub);
			}
			else
			{
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static String replace(String s, String oldSub, String newSub)
	{
		return replace(s, oldSub, newSub, 0);
	}

	public static String replace(
			String s, String oldSub, String newSub, int fromIndex)
	{

		if (s == null)
		{
			return null;
		}

		if ((oldSub == null) || oldSub.equals(StringPool.BLANK))
		{
			return s;
		}

		if (newSub == null)
		{
			newSub = StringPool.BLANK;
		}

		int y = s.indexOf(oldSub, fromIndex);

		if (y >= 0)
		{
			StringBuffer sb = new StringBuffer();

			int length = oldSub.length();
			int x = 0;

			while (x <= y)
			{
				sb.append(s.substring(x, y));
				sb.append(newSub);

				x = y + length;
				y = s.indexOf(oldSub, x);
			}

			sb.append(s.substring(x));

			return sb.toString();
		}
		else
		{
			return s;
		}
	}

	public static String replace(
			String s, String begin, String end, Map<String, String> values)
	{

		StringBuffer sb = replaceToStringBuffer(s, begin, end, values);

		return sb.toString();
	}

	public static String replace(String s, String[] oldSubs, String[] newSubs)
	{
		if ((s == null) || (oldSubs == null) || (newSubs == null))
		{
			return null;
		}

		if (oldSubs.length != newSubs.length)
		{
			return s;
		}

		for (int i = 0; i < oldSubs.length; i++)
		{
			s = replace(s, oldSubs[i], newSubs[i]);
		}

		return s;
	}

	public static String replace(
			String s, String[] oldSubs, String[] newSubs, boolean exactMatch)
	{

		if ((s == null) || (oldSubs == null) || (newSubs == null))
		{
			return null;
		}

		if (oldSubs.length != newSubs.length)
		{
			return s;
		}

		if (!exactMatch)
		{
			replace(s, oldSubs, newSubs);
		}
		else
		{
			for (int i = 0; i < oldSubs.length; i++)
			{
				s = s.replaceAll("\\b" + oldSubs[i] + "\\b", newSubs[i]);
			}
		}

		return s;
	}

	public static String replaceFirst(String s, char oldSub, char newSub)
	{
		if (s == null)
		{
			return null;
		}

		return replaceFirst(s, String.valueOf(oldSub), String.valueOf(newSub));
	}

	public static String replaceFirst(String s, char oldSub, String newSub)
	{
		if ((s == null) || (newSub == null))
		{
			return null;
		}

		return replaceFirst(s, String.valueOf(oldSub), newSub);
	}

	public static String replaceFirst(String s, String oldSub, String newSub)
	{
		if ((s == null) || (oldSub == null) || (newSub == null))
		{
			return null;
		}

		if (oldSub.equals(newSub))
		{
			return s;
		}

		int y = s.indexOf(oldSub);

		if (y >= 0)
		{
			return s.substring(0, y).concat(newSub).concat(
					s.substring(y + oldSub.length()));
		}
		else
		{
			return s;
		}
	}

	public static String replaceFirst(
			String s, String[] oldSubs, String[] newSubs)
	{

		if ((s == null) || (oldSubs == null) || (newSubs == null))
		{
			return null;
		}

		if (oldSubs.length != newSubs.length)
		{
			return s;
		}

		for (int i = 0; i < oldSubs.length; i++)
		{
			s = replaceFirst(s, oldSubs[i], newSubs[i]);
		}

		return s;
	}

	public static String replaceLast(String s, char oldSub, char newSub)
	{
		if (s == null)
		{
			return null;
		}

		return replaceLast(s, String.valueOf(oldSub), String.valueOf(newSub));
	}

	public static String replaceLast(String s, char oldSub, String newSub)
	{
		if ((s == null) || (newSub == null))
		{
			return null;
		}

		return replaceLast(s, String.valueOf(oldSub), newSub);
	}

	public static String replaceLast(String s, String oldSub, String newSub)
	{
		if ((s == null) || (oldSub == null) || (newSub == null))
		{
			return null;
		}

		if (oldSub.equals(newSub))
		{
			return s;
		}

		int y = s.lastIndexOf(oldSub);

		if (y >= 0)
		{
			return s.substring(0, y).concat(newSub).concat(
					s.substring(y + oldSub.length()));
		}
		else
		{
			return s;
		}
	}

	public static String replaceLast(
			String s, String[] oldSubs, String[] newSubs)
	{

		if ((s == null) || (oldSubs == null) || (newSubs == null))
		{
			return null;
		}

		if (oldSubs.length != newSubs.length)
		{
			return s;
		}

		for (int i = 0; i < oldSubs.length; i++)
		{
			s = replaceLast(s, oldSubs[i], newSubs[i]);
		}

		return s;
	}

	public static StringBuffer replaceToStringBuffer(
			String s, String begin, String end, Map<String, String> values)
	{

		if ((s == null) || (begin == null) || (end == null) ||
				(values == null) || (values.size() == 0))
		{

			return new StringBuffer(s);
		}

		StringBuffer sb = new StringBuffer(values.size() * 2 + 1);

		int pos = 0;

		while (true)
		{
			int x = s.indexOf(begin, pos);
			int y = s.indexOf(end, x + begin.length());

			if ((x == -1) || (y == -1))
			{
				sb.append(s.substring(pos, s.length()));

				break;
			}
			else
			{
				sb.append(s.substring(pos, x));

				String oldValue = s.substring(x + begin.length(), y);

				String newValue = values.get(oldValue);

				if (newValue == null)
				{
					newValue = oldValue;
				}

				sb.append(newValue);

				pos = y + end.length();
			}
		}

		return sb;
	}

	public static String reverse(String s)
	{
		if (s == null)
		{
			return null;
		}

		char[] chars = s.toCharArray();
		char[] reverse = new char[chars.length];

		for (int i = 0; i < chars.length; i++)
		{
			reverse[i] = chars[chars.length - i - 1];
		}

		return new String(reverse);
	}

	public static String safePath(String path)
	{
		return replace(path, StringPool.DOUBLE_SLASH, StringPool.SLASH);
	}

	public static String shorten(String s)
	{
		return shorten(s, 20);
	}

	public static String shorten(String s, int length)
	{
		return shorten(s, length, "...");
	}

	public static String shorten(String s, int length, String suffix)
	{
		if ((s == null) || (suffix == null))
		{
			return null;
		}

		if (s.length() > length)
		{
			for (int j = length; j >= 0; j--)
			{
				if (Character.isWhitespace(s.charAt(j)))
				{
					length = j;

					break;
				}
			}

			String temp = s.substring(0, length);

			s = temp.concat(suffix);
		}

		return s;
	}

	public static String shorten(String s, String suffix)
	{
		return shorten(s, 20, suffix);
	}

	public static String[] split(String s)
	{
		return split(s, CharPool.COMMA);
	}

	public static boolean[] split(String s, boolean x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static double[] split(String s, double x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static float[] split(String s, float x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static int[] split(String s, int x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static long[] split(String s, long x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static short[] split(String s, short x)
	{
		return split(s, StringPool.COMMA, x);
	}

	public static String[] split(String s, char delimiter)
	{
		if (Validator.isNull(s))
		{
			return _emptyString;
		}

		s = s.trim();

		if (s.length() == 0)
		{
			return _emptyString;
		}

		if ((delimiter == CharPool.RETURN) || (delimiter == CharPool.NEW_LINE))
		{
			return splitLines(s);
		}

		List<String> nodeValues = new ArrayList<String>();

		int offset = 0;
		int pos = s.indexOf(delimiter, offset);

		while (pos != -1)
		{
			nodeValues.add(s.substring(offset, pos));

			offset = pos + 1;
			pos = s.indexOf(delimiter, offset);
		}

		if (offset < s.length())
		{
			nodeValues.add(s.substring(offset));
		}

		return nodeValues.toArray(new String[nodeValues.size()]);
	}

	public static String[] split(String s, String delimiter)
	{
		if ((Validator.isNull(s)) || (delimiter == null) ||
				(delimiter.equals(StringPool.BLANK)))
		{

			return _emptyString;
		}

		s = s.trim();

		if (s.equals(delimiter))
		{
			return _emptyString;
		}

		if (delimiter.length() == 1)
		{
			return split(s, delimiter.charAt(0));
		}

		List<String> nodeValues = new ArrayList<String>();

		int offset = 0;
		int pos = s.indexOf(delimiter, offset);

		while (pos != -1)
		{
			nodeValues.add(s.substring(offset, pos));

			offset = pos + delimiter.length();
			pos = s.indexOf(delimiter, offset);
		}

		if (offset < s.length())
		{
			nodeValues.add(s.substring(offset));
		}

		return nodeValues.toArray(new String[nodeValues.size()]);
	}

	public static boolean[] split(String s, String delimiter, boolean x)
	{
		String[] array = split(s, delimiter);
		boolean[] newArray = new boolean[array.length];

		for (int i = 0; i < array.length; i++)
		{
			boolean value = x;

			try
			{
				value = Boolean.valueOf(array[i]).booleanValue();
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static double[] split(String s, String delimiter, double x)
	{
		String[] array = split(s, delimiter);
		double[] newArray = new double[array.length];

		for (int i = 0; i < array.length; i++)
		{
			double value = x;

			try
			{
				value = Double.parseDouble(array[i]);
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static float[] split(String s, String delimiter, float x)
	{
		String[] array = split(s, delimiter);
		float[] newArray = new float[array.length];

		for (int i = 0; i < array.length; i++)
		{
			float value = x;

			try
			{
				value = Float.parseFloat(array[i]);
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static int[] split(String s, String delimiter, int x)
	{
		String[] array = split(s, delimiter);
		int[] newArray = new int[array.length];

		for (int i = 0; i < array.length; i++)
		{
			int value = x;

			try
			{
				value = Integer.parseInt(array[i]);
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static long[] split(String s, String delimiter, long x)
	{
		String[] array = split(s, delimiter);
		long[] newArray = new long[array.length];

		for (int i = 0; i < array.length; i++)
		{
			long value = x;

			try
			{
				value = Long.parseLong(array[i]);
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static short[] split(String s, String delimiter, short x)
	{
		String[] array = split(s, delimiter);
		short[] newArray = new short[array.length];

		for (int i = 0; i < array.length; i++)
		{
			short value = x;

			try
			{
				value = Short.parseShort(array[i]);
			}
			catch (Exception e)
			{
			}

			newArray[i] = value;
		}

		return newArray;
	}

	public static String[] splitLines(String s)
	{
		if (Validator.isNull(s))
		{
			return _emptyString;
		}

		s = s.trim();

		List<String> lines = new ArrayList<String>();

		int lastIndex = 0;

		while (true)
		{
			int returnIndex = s.indexOf(CharPool.RETURN, lastIndex);
			int newLineIndex = s.indexOf(CharPool.NEW_LINE, lastIndex);

			if ((returnIndex == -1) && (newLineIndex == -1))
			{
				break;
			}

			if (returnIndex == -1)
			{
				lines.add(s.substring(lastIndex, newLineIndex));

				lastIndex = newLineIndex + 1;
			}
			else if (newLineIndex == -1)
			{
				lines.add(s.substring(lastIndex, returnIndex));

				lastIndex = returnIndex + 1;
			}
			else if (newLineIndex < returnIndex)
			{
				lines.add(s.substring(lastIndex, newLineIndex));

				lastIndex = newLineIndex + 1;
			}
			else
			{
				lines.add(s.substring(lastIndex, returnIndex));

				lastIndex = returnIndex + 1;

				if (lastIndex == newLineIndex)
				{
					lastIndex++;
				}
			}
		}

		if (lastIndex < s.length())
		{
			lines.add(s.substring(lastIndex));
		}

		return lines.toArray(new String[lines.size()]);
	}

	public static boolean startsWith(String s, char begin)
	{
		return startsWith(s, (new Character(begin)).toString());
	}

	public static boolean startsWith(String s, String start)
	{
		if ((s == null) || (start == null))
		{
			return false;
		}

		if (start.length() > s.length())
		{
			return false;
		}

		String temp = s.substring(0, start.length());

		if (temp.equalsIgnoreCase(start))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Return the number of starting letters that s1 and s2 have in common
	 * before they deviate.
	 * 
	 * @return the number of starting letters that s1 and s2 have in common
	 *         before they deviate
	 */
	public static int startsWithWeight(String s1, String s2)
	{
		if ((s1 == null) || (s2 == null))
		{
			return 0;
		}

		char[] chars1 = s1.toCharArray();
		char[] chars2 = s2.toCharArray();

		int i = 0;

		for (; (i < chars1.length) && (i < chars2.length); i++)
		{
			if (chars1[i] != chars2[i])
			{
				break;
			}
		}

		return i;
	}

	public static String strip(String s, char remove)
	{
		if (s == null)
		{
			return null;
		}

		int x = s.indexOf(remove);

		if (x < 0)
		{
			return s;
		}

		int y = 0;

		StringBuffer sb = new StringBuffer(s.length());

		while (x >= 0)
		{
			sb.append(s.subSequence(y, x));

			y = x + 1;

			x = s.indexOf(remove, y);
		}

		sb.append(s.substring(y));

		return sb.toString();
	}

	public static String stripBetween(String s, String begin, String end)
	{
		if ((s == null) || (begin == null) || (end == null))
		{
			return s;
		}

		StringBuffer sb = new StringBuffer(s.length());

		int pos = 0;

		while (true)
		{
			int x = s.indexOf(begin, pos);
			int y = s.indexOf(end, x + begin.length());

			if ((x == -1) || (y == -1))
			{
				sb.append(s.substring(pos, s.length()));

				break;
			}
			else
			{
				sb.append(s.substring(pos, x));

				pos = y + end.length();
			}
		}

		return sb.toString();
	}

	public static String toCharCode(String s)
	{
		StringBuffer sb = new StringBuffer(s.length());

		for (int i = 0; i < s.length(); i++)
		{
			sb.append(s.codePointAt(i));
		}

		return sb.toString();
	}

	public static String toHexString(int i)
	{
		char[] buffer = new char[8];

		int index = 8;

		do
		{
			buffer[--index] = _HEX_DIGITS[i & 15];

			i >>>= 4;
		}
		while (i != 0);

		return new String(buffer, index, 8 - index);
	}

	public static String toHexString(long l)
	{
		char[] buffer = new char[16];

		int index = 16;

		do
		{
			buffer[--index] = _HEX_DIGITS[(int) (l & 15)];

			l >>>= 4;
		}
		while (l != 0);

		return new String(buffer, index, 16 - index);
	}

	public static String toHexString(byte[] bytes, int index, int count)
	{
		StringBuffer localStringBuffer = new StringBuffer(count * 2);
		int endIndex = index + count;
		for (int j = index; j < endIndex; ++j)
		{
			int k = (bytes[j] & 0xF0) >>> 4;
			int l = bytes[j] & 0xF;
			localStringBuffer.append(_HEX_DIGITS[k]);
			localStringBuffer.append(_HEX_DIGITS[l]);
		}
		return localStringBuffer.toString();
	}

	public static String toHexString(Object obj)
	{
		if (obj instanceof Integer)
		{
			return toHexString(((Integer) obj).intValue());
		}
		else if (obj instanceof Long)
		{
			return toHexString(((Long) obj).longValue());
		}
		else
		{
			return String.valueOf(obj);
		}
	}

	private static boolean _isTrimable(char c, char[] exceptions)
	{
		if ((exceptions != null) && (exceptions.length > 0))
		{
			for (char exception : exceptions)
			{
				if (c == exception)
				{
					return false;
				}
			}
		}

		return Character.isWhitespace(c);
	}

	public static String trim(String s)
	{
		return trim(s, null);
	}

	public static String trim(String s, char c)
	{
		return trim(s, new char[] { c });
	}

	public static String trim(String s, char[] exceptions)
	{
		if (s == null)
		{
			return null;
		}

		char[] chars = s.toCharArray();

		int len = chars.length;

		int x = 0;
		int y = chars.length;

		for (int i = 0; i < len; i++)
		{
			char c = chars[i];

			if (_isTrimable(c, exceptions))
			{
				x = i + 1;
			}
			else
			{
				break;
			}
		}

		for (int i = len - 1; i >= 0; i--)
		{
			char c = chars[i];

			if (_isTrimable(c, exceptions))
			{
				y = i;
			}
			else
			{
				break;
			}
		}

		if ((x != 0) || (y != len))
		{
			return s.substring(x, y);
		}
		else
		{
			return s;
		}
	}

	public static String trimLeading(String s)
	{
		return trimLeading(s, null);
	}

	public static String trimLeading(String s, char c)
	{
		return trimLeading(s, new char[] { c });
	}

	public static String trimLeading(String s, char[] exceptions)
	{
		if (s == null)
		{
			return null;
		}

		char[] chars = s.toCharArray();

		int len = chars.length;

		int x = 0;
		int y = chars.length;

		for (int i = 0; i < len; i++)
		{
			char c = chars[i];

			if (_isTrimable(c, exceptions))
			{
				x = i + 1;
			}
			else
			{
				break;
			}
		}

		if ((x != 0) || (y != len))
		{
			return s.substring(x, y);
		}
		else
		{
			return s;
		}
	}

	public static String trimTrailing(String s)
	{
		return trimTrailing(s, null);
	}

	public static String trimTrailing(String s, char c)
	{
		return trimTrailing(s, new char[] { c });
	}

	public static String trimTrailing(String s, char[] exceptions)
	{
		if (s == null)
		{
			return null;
		}

		char[] chars = s.toCharArray();

		int len = chars.length;

		int x = 0;
		int y = chars.length;

		for (int i = len - 1; i >= 0; i--)
		{
			char c = chars[i];

			if (_isTrimable(c, exceptions))
			{
				y = i;
			}
			else
			{
				break;
			}
		}

		if ((x != 0) || (y != len))
		{
			return s.substring(x, y);
		}
		else
		{
			return s;
		}
	}

	public static String unquote(String s)
	{
		if (Validator.isNull(s))
		{
			return s;
		}

		if ((s.charAt(0) == CharPool.APOSTROPHE) &&
				(s.charAt(s.length() - 1) == CharPool.APOSTROPHE))
		{

			return s.substring(1, s.length() - 1);
		}
		else if ((s.charAt(0) == CharPool.QUOTE) &&
				(s.charAt(s.length() - 1) == CharPool.QUOTE))
		{

			return s.substring(1, s.length() - 1);
		}

		return s;
	}

	public static String upperCase(String s)
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			return s.toUpperCase();
		}
	}

	public static String upperCaseFirstLetter(String s)
	{
		char[] chars = s.toCharArray();

		if ((chars[0] >= 97) && (chars[0] <= 122))
		{
			chars[0] = (char) (chars[0] - 32);
		}

		return new String(chars);
	}

	public static String valueOf(Object obj)
	{
		return String.valueOf(obj);
	}

	public static String nvl(Object objInput, String defaultValue)
	{
		return objInput == null ? defaultValue : objInput.toString();
	}

	public static String nvl(String value, String nullValue)
	{
		return value == null ? nullValue : value;
	}

	// //////////////////////////////////////////////////////
	/**
	 * Convert a string to string array base on separator
	 * 
	 * @param strSource
	 *            String to convert
	 * @param strSeparator
	 *            separator symbol
	 * @return String array after convert
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	@SuppressWarnings("rawtypes")
	public static String[] toStringArray(String strSource, String strSeparator)
	{
		Vector vtReturn = toStringVector(nvl(strSource, ""), strSeparator);

		String[] strReturn = new String[vtReturn.size()];

		for (int iIndex = 0; iIndex < strReturn.length; iIndex++)
		{
			strReturn[iIndex] = (String) vtReturn.elementAt(iIndex);
		}

		return strReturn;
	}
	
	@SuppressWarnings("rawtypes")
	public static int[] toIntegerArray(String strSource, String strSeparator)
	{
		Vector vtReturn = toStringVector(nvl(strSource, ""), strSeparator);

		int[] iReturn = new int[vtReturn.size()];

		for (int iIndex = 0; iIndex < iReturn.length; iIndex++)
		{
			try
			{
				iReturn[iIndex] = (Integer) vtReturn.elementAt(iIndex);
			}
			catch (Exception e)
			{
				
			}
		}

		return iReturn;
	}

	// //////////////////////////////////////////////////////
	/**
	 * Convert a string to string array base on separator
	 * 
	 * @param strSource
	 *            String to convert
	 * @param strSeparator
	 *            separator symbol
	 * @return String array after convert
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Vector toStringVector(String strSource, String strSeparator)
	{
		if (strSource.equals(""))
		{
			return new Vector();
		}

		Vector vtReturn = new Vector();
		int iIndex = 0;
		int iLastIndex = 0;
		while ((iIndex = strSource.indexOf(strSeparator, iLastIndex)) >= 0)
		{
			String element = strSource.substring(iLastIndex, iIndex).trim();

			if (!element.equals(""))
			{
				vtReturn.addElement(strSource.substring(iLastIndex, iIndex).trim());
			}

			iLastIndex = iIndex + strSeparator.length();
		}
		if (iLastIndex <= strSource.length())
			vtReturn.addElement(strSource.substring(iLastIndex, strSource.length()).trim());
		return vtReturn;
	}

	public static ArrayList<String> toList(String source, String separator)
	{
		ArrayList<String> result = new ArrayList<String>();

		if (source.equals(""))
		{
			return result;
		}
		else
		{
			int index = 0;
			int last = 0;

			while ((index = source.indexOf(separator, last)) >= 0)
			{
				String element = source.substring(last, index).trim();

				if (!element.equals(""))
				{
					result.add(source.substring(last, index).trim());
				}

				last = index + separator.length();
			}

			if (last <= source.length())
			{
				result.add(source.substring(last, source.length()).trim());
			}

			return result;
		}
	}

	// //////////////////////////////////////////////////////
	/**
	 * Format date object
	 * 
	 * @param date
	 *            date to format
	 * @param pattern
	 *            format pattern
	 * @return formatted string
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	public static String format(Date date, String pattern)
	{
		if (date == null)
		{
			return null;
		}

		java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(pattern);

		return fmt.format(date);
	}

	// //////////////////////////////////////////////////////
	/**
	 * Format long number
	 * 
	 * @param value
	 *            number to format
	 * @param pattern
	 *            format pattern
	 * @return formatted string
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	public static String format(long value, String pattern)
	{
		java.text.DecimalFormat fmt = new java.text.DecimalFormat(pattern);

		return fmt.format(value);
	}

	// //////////////////////////////////////////////////////
	/**
	 * Format double number
	 * 
	 * @param value
	 *            number to format
	 * @param pattern
	 *            format pattern
	 * @return formatted string
	 * @author Thai Hoang Hiep
	 */
	// //////////////////////////////////////////////////////
	public static String format(double value, String pattern)
	{
		java.text.DecimalFormat fmt = new java.text.DecimalFormat(pattern);

		return fmt.format(value);
	}

	public static String toString(Properties properties)
	{
		StringBuffer result = new StringBuffer();

		Enumeration<?> propNames = properties.propertyNames();

		for (; propNames.hasMoreElements();)
		{
			String key = (String) propNames.nextElement();

			result.append(key);
			result.append("=");
			result.append(properties.getProperty(key));
			result.append("\r\n");
		}

		return result.toString();
	}

	public static String toString(Object object, String[] properties)
	{
		if (object == null)
		{
			return "";
		}

		Class<?> type = object.getClass();
		Method[] methods = type.getMethods();

		StringBuilder sbLog = new StringBuilder();

		for (int i = 0; i < methods.length; i++)
		{
			if (!methods[i].getName().startsWith("get"))
			{
				continue;
			}

			if ((properties != null) && (properties.length > 0))
			{
				boolean found = false;

				for (int j = 0; !found && (j < properties.length); j++)
				{

				}
			}

			String member = "";

			try
			{
				Object value = methods[i].invoke(object, new Object[] {});

				// Object value = methods[i].invoke(object);
				
				if (value == null)
				{

				}
				else if (value instanceof Date || value instanceof Calendar)
				{
					member = (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS")).format(value);
				}
				else 
				{
					member = value.toString();
				}

				sbLog.append(methods[i].getName().substring(3));
				sbLog.append("=");
				sbLog.append(member);
				sbLog.append(" | ");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return sbLog.toString();
	}

	public static String toString(Object object)
	{
		return toString(object, null);
	}
}
