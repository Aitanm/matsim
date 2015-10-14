/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStopsParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.dziemke.analysis.srv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;

import playground.dziemke.analysis.Trip;
import playground.dziemke.cemdapMatsimCadyts.Zone;

/**
 * @author dziemke
 * This class parses the SrV2008 trip file and creates a map with Trip object that
 * contain the relevant information
 */
public class SrVTripParser {

	private final static Logger log = Logger.getLogger(SrVTripParser.class);
	
	private Map<Id<Trip>, Trip> trips = new HashMap<Id<Trip>, Trip>();
	
	private static final String HOUSEHOLD_ID = "HHNR";
	private static final String PERSON_ID = "PNR";
	private static final String TRIP_ID = "WNR";
	// activity end corresponds to start of trip
	private static final String DEPARTURE_ZONE_ID = "START_TEILBEZIRK2";
	private static final String ACTIVITY_END_ACT_TYPE = "V_START_ZWECK";
	private static final String ACTIVITY_START_ACT_TYPE = "V_ZWECK";
	private static final String USE_HOUSEHOLD_CAR = "V_HHPKW_F";
	private static final String USE_OTHER_CAR = "V_ANDPKW_F";
	private static final String USE_HOUSEHOLD_CAR_POOL = "V_HHPKW_MF";
	private static final String USE_OTHER_CAR_POOL = "V_ANDPKW_MF";
	private static final String ARRIVAL_ZONE_ID = "ZIEL_TEILBEZIRK2";
	// departure time corresponds to "beginn" of trip
	private static final String DEPARTURE_TIME = "V_BEGINN";
	private static final String ARRIVAL_TIME = "V_ANKUNFT";
	private static final String DISTANCE_BEELINE = "V_LAENGE";
	//private static final String MODE = "E_HVM";
	private static final String DURATION = "E_DAUER";
	private static final String SPEED = "E_GESCHW";
	private static final String DISTANCE_ROUTED_FASTEST = "E_LAENGE_SCHNELL";
	private static final String DISTANCE_ROUTED_SHORTEST = "E_LAENGE_KUERZ";
	
	private static final String WEIGHT = "GEWICHT_W";
	

	public SrVTripParser() {
	}
	
	
	public final void parse(String srvTripFile) {
		
		int lineCount = 0;
		
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(srvTripFile);

			// header
			String currentLine = bufferedReader.readLine();
			lineCount++;
			String[] heads = currentLine.split(";", -1);
			Map<String,Integer> columnNumbers = new LinkedHashMap<String,Integer>(heads.length);
			for (int i = 0; i < heads.length; i++) {
				columnNumbers.put(heads[i],i);
				//System.out.println("Column No.=" + i + " has an entry with title=" + heads[i]);
			}
			
			// read data and write relevant information to a new Trip object
			while ((currentLine = bufferedReader.readLine()) != null) {
				//String[] entries = curr_line.split("\t", -1);
				String[] entries = currentLine.split(";", -1);
				lineCount++;
		
				// test file (it.150 of run.104) has 111229 lines altogether
				// line count is correct since starting with "0" take in account the fact that
				// the first line is the header, which may not be counted
				
				if (lineCount % 100000 == 0) {
					log.info(lineCount+ " lines read in so far.");
					Gbl.printMemoryUsage();
				}
				
				Trip trip = new Trip();
				Id<Household> householdId = Id.create(entries[columnNumbers.get(HOUSEHOLD_ID)], Household.class);
				Id<Person> personId = Id.create(entries[columnNumbers.get(PERSON_ID)], Person.class);
				Id<Trip> tripId = Id.create(entries[columnNumbers.get(TRIP_ID)], Trip.class);
				//String activityEndActType = new String(entries[columnNumbers.get(ACTIVITY_END_ACT_TYPE)]);
				String activityEndActType = transformActType(new Integer(entries[columnNumbers.get(ACTIVITY_END_ACT_TYPE)]));
				Id<Zone> departureZoneId = Id.create(entries[columnNumbers.get(DEPARTURE_ZONE_ID)], Zone.class);
				Double departureTime = new Double(entries[columnNumbers.get(DEPARTURE_TIME)]);
				Integer useHouseholdCar = new Integer(entries[columnNumbers.get(USE_HOUSEHOLD_CAR)]);
				Integer useOtherCar = new Integer(entries[columnNumbers.get(USE_OTHER_CAR)]);
				Integer useHouseholdCarPool = new Integer(entries[columnNumbers.get(USE_HOUSEHOLD_CAR_POOL)]);
				Integer useOtherCarPool = new Integer(entries[columnNumbers.get(USE_OTHER_CAR_POOL)]);
				//Integer mode = new Integer(entries[columnNumbers.get(MODE)]);
				//String mode = new String(entries[columnNumbers.get(MODE)]);
				Double distanceBeeline = new Double(entries[columnNumbers.get(DISTANCE_BEELINE)]);
				Double distanceRoutedFastest = new Double(entries[columnNumbers.get(DISTANCE_ROUTED_FASTEST)]);
				Double distanceRoutedShortest = new Double(entries[columnNumbers.get(DISTANCE_ROUTED_SHORTEST)]);
				Double speed= new Double(entries[columnNumbers.get(SPEED)]);
				Double duration = new Double(entries[columnNumbers.get(DURATION)]);
				Id<Zone> arrivalZoneId = Id.create(entries[columnNumbers.get(ARRIVAL_ZONE_ID)], Zone.class);
				Double arrivalTime = new Double(entries[columnNumbers.get(ARRIVAL_TIME)]);
				// String activityStartActType = new String(entries[columnNumbers.get(ACTIVITY_START_ACT_TYPE)]);
				String activityStartActType = transformActType(new Integer(entries[columnNumbers.get(ACTIVITY_START_ACT_TYPE)]));
				
				Double weight = new Double(entries[columnNumbers.get(WEIGHT)]);
				
								
				personId = Id.create(householdId + "_" + personId, Person.class);
				trip.setPersonId(personId);
				tripId = Id.create(personId + "_" + tripId, Trip.class);
				trip.setTripId(tripId);
				trip.setActivityEndActType(activityEndActType);
				trip.setDepartureZoneId(departureZoneId);
				trip.setDepartureTime(departureTime);
				//trip.setDepartureLegMode(departureLegMode);
				trip.setUseHouseholdCar(useHouseholdCar);
				trip.setUseOtherCar(useOtherCar);
				trip.setUseHouseholdCarPool(useHouseholdCarPool);
				trip.setUseOtherCarPool(useOtherCarPool);
				//trip.setMode(mode);
				trip.setDistanceBeeline(distanceBeeline);
				trip.setDistanceRoutedFastest(distanceRoutedFastest);
				trip.setDistanceRoutedShortest(distanceRoutedShortest);
				trip.setSpeed(speed);
				trip.setDuration(duration);
				trip.setArrivalZoneId(arrivalZoneId);
				trip.setArrivalTime(arrivalTime);
				//trip.setArrivalLegMode(arrivalLegMode);
				trip.setActivityStartActType(activityStartActType);
				
				trip.setWeight(weight);
				
//				System.out.println("personId = " + trip.getDriverId());
//				System.out.println("tripId = " + trip.getTripId());
//				System.out.println("tripActivityEndType = " + trip.getActivityEndActType());
//				System.out.println("departureZoneId = " + trip.getDepartureZoneId());
//				System.out.println("departureTime = " + trip.getDepartureTime());
//				System.out.println("useHouseholdCar = " + trip.getUseHouseholdCar());
//				System.out.println("useOtherCar = " + trip.getUseOtherCar());
//				System.out.println("mode = " + trip.getMode());
//				System.out.println("distanceBeeline = " + trip.getDistanceBeeline());
//				System.out.println("distanceRoutedFastest = " + trip.getDistanceRoutedFastest());
//				System.out.println("distanceRoutedShortest = " + trip.getDistanceRoutedShortest());
//				System.out.println("speed = " + trip.getSpeed());
//				System.out.println("duration = " + trip.getDuration());
//				double time = trip.getArrivalTime() - trip.getDepartureTime();
//				System.out.println("calculated duration = " + time);
//				double calculatedSpeed = trip.getDistanceBeeline() / time * 60.;
//				System.out.println("calculated speed = " + calculatedSpeed);
//				System.out.println("arrivalZoneId = " + trip.getArrivalZoneId());
//				System.out.println("arrivalTime = " + trip.getArrivalTime());
//				System.out.println("activityStartActType = " + trip.getActivityStartActType());
//				System.out.println("---------------------------------------------------");
				
				trips.put(tripId, trip);
			}
		} catch (IOException e) {
			log.error(new Exception(e));
			//Gbl.errorMsg(e);
		}
	}
	
	
	// see p.45 Aufbereitungsbericht
	private final String transformActType(int actTypeCode) {
		switch (actTypeCode) {
		case -9: return "other";
		case 1: return "work";
		case 2: return "work";
		case 3: return "other";
		case 4: return "other";
		//case 4: return "educ";
		case 5: return "other";
		//case 5: return "educ";
		case 6: return "other";
		//case 6: return "educ";
		case 7: return "shop";
		case 8: return "shop";
		case 9: return "other";
		case 10: return "leis";
		case 11: return "leis";
		case 12: return "leis";
		case 13: return "leis";
		case 14: return "leis";
		case 15: return "leis";
		case 16: return "leis";
		case 17: return "home";
		case 18: return "other";
		default:
			log.error(new IllegalArgumentException("actTypeNo="+actTypeCode+" not allowed."));
			//Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeCode+" not allowed."));
			return null;
		}
	}
	
	
	public Map<Id<Trip>, Trip> getTrips() {
		return this.trips;
	}
}
