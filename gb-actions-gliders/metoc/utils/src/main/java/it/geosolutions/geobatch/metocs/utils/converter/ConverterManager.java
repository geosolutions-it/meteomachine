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
package it.geosolutions.geobatch.metocs.utils.converter;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ucar.units.ConversionException;
import ucar.units.Converter;
import ucar.units.NameException;
import ucar.units.NoSuchUnitException;
import ucar.units.ScaledUnit;
import ucar.units.Unit;
import ucar.units.UnitDB;
import ucar.units.UnitDBAccessException;
import ucar.units.UnitDBException;
import ucar.units.UnitDBManager;
import ucar.units.UnitExistsException;
import ucar.units.UnitName;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class ConverterManager {
	final UnitDB unitDB;// UnitSystemImpl.getBaseUnitDB();

	public ConverterManager() throws UnitDBException {
		// UnitDBImpl db=new UnitDBManager().instance();
		// UnitSystemImpl db= new UnitSystemImpl();
		unitDB = UnitDBManager.instance();// UnitSystemImpl.getBaseUnitDB();
	}
	
	/**
	 * print the entire database
	 */
	public String toString(){
		final StringBuffer buf=new StringBuffer();
		Iterator<?> it=unitDB.getIterator();
		buf.append("\tUnitDB:\n----------------");
		long num=0;
		while (it.hasNext()){
			buf.append("\n\t"+(++num)+"\t"+it.next().toString());
		}
		buf.append("\n----------------");
		return buf.toString();
		
	}
	public boolean addAlias(final Map<String , String> aliasMap){
		boolean status=true;
		Set<Map.Entry<String,String>> entrySet=aliasMap.entrySet();
		Iterator<Entry<String,String>> it=entrySet.iterator();
		while(it.hasNext()){
			final Entry<String,String> entry=it.next();
			status=(status && addAlias(entry.getKey(),entry.getValue()));
		}
		// return global status
		return status;
	}
	
	/**
	 * Adds an alias for a unit to the database.
	 * @param alias
	 * @param varName
	 */
	public boolean addAlias(final String alias, final String varName){
		try {
			// alias, symbol, plural
			//unitDB.addAlias(alias, alias, alias, varName);
			
			unitDB.addAlias(alias, varName);
			return true;
		} catch (NoSuchUnitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnitExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnitDBAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @param fromUnit
	 * @param toUnitName
	 * @return the matching converter or null
	 */
	public Converter getConverter(final String fromUnitName, final String toUnitName){
		final Unit fromUnitU=get(fromUnitName);
		final Unit toUnitU=get(toUnitName);
		if (fromUnitU!=null && toUnitU!=null){
			try {
				return fromUnitU.getConverterTo(toUnitU);
			} catch (ConversionException ce){
				// TODO Auto-generated catch block
				ce.printStackTrace();	
			}
		}
		return null;
	}

	/**
	 * Gets the unit in the database whose name, plural, or symbol, match an identifier.
	 * @param name
	 * @return the matching unit or null
	 */
	public Unit get(final String name){
		try {
			return unitDB.get(name);
		} catch (UnitDBAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds an alias for a unit to the database.
	 * @param alias
	 * @param varName
	 */
	public boolean addUnit(final Unit unit){
		try {
			// alias, symbol, plural
			//unitDB.addAlias(alias, alias, alias, varName);
			unitDB.addUnit(unit);
			return true;
		} catch (UnitExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnitDBAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	
	// tests
	public static void main(final String[] args){
		try {
			
			ConverterManager manager=new ConverterManager();
			
			System.out.println(manager.toString());
			
			Converter converter=manager.getConverter("Kelvin", "Celsius");
			System.out.println("from 273.15 Kelvin to :"+converter.convert(273.15)+ " Celsius");
			
			 converter=manager.getConverter("°K", "°C");
			 if (converter!=null)
				 System.out.println("from 1 to :"+converter.convert(1));
			 else
				 System.out.println("Converter is null [not found]");
			 
			 System.out.println("Adding aliases...");
			 
			 manager.addAlias("°K", "Kelvin");
			 manager.addAlias("°C", "Celsius");
			 
			 System.out.println(manager.toString());
			 
			 System.out.println("Search again...");
			 
			 converter=manager.getConverter("°K", "°C");
			 if (converter!=null)
				 System.out.println("from 273.15 Kelvin to :"+converter.convert(273.15)+ " Celsius");	 
			 else
				 System.out.println("Converter is null [not found]");
			 
			 
			 try {
				manager.addUnit(new ScaledUnit(100,manager.get("Pa"), UnitName.newUnitName("hPa")));
			} catch (NameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 converter=manager.getConverter("Pa", "hPa");
			 if (converter!=null)
				 System.out.println("from 1 Pa to :"+converter.convert(1)+ " hectoPascal");
			 else
				 System.out.println("Converter is null [not found]");
			 
		} catch (UnitDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
