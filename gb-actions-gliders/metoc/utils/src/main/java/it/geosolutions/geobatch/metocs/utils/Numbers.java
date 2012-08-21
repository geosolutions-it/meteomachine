/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.metocs.utils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class Numbers {
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(Numbers.class);

	public static Object get(Number num) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException {
		// System.out.println("MAX:" +
		// num.getClass().getField("MAX_VALUE").getName());
		// System.out.println("MAX:" +
		// num.getClass().getField("MAX_VALUE").getGenericType());
		// System.out.println("MAX:" +
		// num.getClass().getField("MAX_VALUE").get(num));
		return num.getClass().getField("MAX_VALUE").get(num);
	}

	/**
	 * @note be careful use this method on BigInteger, others Numbers may throw
	 *       NoSuchMethodException
	 * @param <NUM>
	 * @param num
	 * @return
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public static <NUM extends Number> Number negateValue(final NUM num)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		final Class<? extends Number> numClass = num.getClass();
		return numClass.cast(numClass.getMethod("negate", (Class[]) null)
				.invoke(num, (Object[]) null));
	}

	// public static <NUM extends Number> Number getVal(NUM num) throws
	// NoSuchFieldException,
	// IllegalArgumentException, IllegalAccessException {
	// final Class<? extends Number> numClass = num.getClass();
	// final Field val = numClass.getDeclaredField("value");
	// val.setAccessible(true);
	// return numClass.cast(val.get(num));
	// }

	/**
	 * @param args
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	public static void main(String[] args) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		System.out.println("STARTING");

		Double d = new Double(1);
		// --------------------- MAX/MIN--------------------------
		System.out.println("MAX:"
				+ getStaticFieldValue(Double.class, "MAX_VALUE"));
		System.out.println("MAX:" + Numbers.getMaxValue(d));
		System.out.println("MIN:" + Numbers.getMinValue(d));
		try {
			System.out.println("NEGATE:" + Numbers.negateValue(d));
		} catch (NoSuchMethodException e) {
			System.out
					.println("be careful use this method on BigInteger, others Numbers may throw NoSuchMethodException");
		}
		System.out.println("NEGATE:" + Numbers.negateValue(new BigInteger("10")));
		
		System.out.println("MAX:" + Double.MAX_VALUE);
		System.out.println("MIN:" + Double.MIN_VALUE);
		// --------------------- VAL--------------------------
		System.out.println("Number to string -> Double:" + d);
		return;
	}

	public static <T extends Number & Comparable<T>> int compareTo(T numA,
			T numB) {
		return numA.compareTo(numB);
	}

	public static <NUM extends Number> Number getNaN(Class<NUM> numClass) {
		return getStaticFieldValue(numClass, "NaN");
	}

	public static <NUM extends Number> Number getNaN(NUM num) {
		return getStaticFieldValue(num.getClass(), "NaN");
	}

	public static <NUM extends Number> Number getMaxValue(NUM num) {
		return getStaticFieldValue(num.getClass(), "MAX_VALUE");
	}

	public static <NUM extends Number> Number getMaxValue(Class<NUM> numClass) {
		return getStaticFieldValue(numClass, "MAX_VALUE");
	}

	public static <NUM extends Number> Number getMinValue(NUM num) {
		return getStaticFieldValue(num.getClass(), "MIN_VALUE");
	}

	public static <NUM extends Number> Number getMinValue(Class<NUM> numClass) {
		return getStaticFieldValue(numClass, "MIN_VALUE");
	}

	public static <NUM extends Number> Number getStaticFieldValue(
			Class<NUM> num, String field) {
		if (num != null) {
			final Object obj;
			try {
				obj = num.getField(field).get(null);
				return num.cast(obj);
			} catch (IllegalArgumentException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			} catch (SecurityException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			} catch (IllegalAccessException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			} catch (NoSuchFieldException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			}
		} else if (LOGGER.isErrorEnabled()) {
			LOGGER.error("Unable to getStaticFieldValue from a null class object!");
		}
		return null;
	}

}
