/* *********************************************************************** *
 * project: org.matsim.*
 * MyTollPotentialEventHandler.java
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

package playground.jjoubert.roadpricing.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.jjoubert.roadpricing.senozon.SanralTollFactor;

public class MyTollPotentialEventHandler implements LinkEnterEventHandler{

	private Logger log = Logger.getLogger(MyPatronLinkEntryHandler.class);
	private List<Id<Person>> breaks;
	private List<Id<Link>> linkIds;
	private List<Map<Id,Double>> valueMaps;
	private List<Map<Id,Integer>> countMaps;
	private RoadPricingScheme scheme;

	public MyTollPotentialEventHandler(List<Id<Link>> linkIds, List<Id<Person>> breaks, RoadPricingScheme scheme) {
		this.linkIds = linkIds;
		this.breaks = breaks;
		valueMaps = new ArrayList<Map<Id,Double>>(breaks.size());
		countMaps = new ArrayList<Map<Id,Integer>>(breaks.size());
		for(int i = 0; i < breaks.size(); i++){
			valueMaps.add(new HashMap<Id, Double>());
			countMaps.add(new HashMap<Id, Integer>());
		}
		this.scheme = scheme;
	}
	
	public List<Map<Id, Double>> getValueMaps(){
		return valueMaps;
	}
	
	public List<Map<Id,Integer>> getCountMaps(){
		return countMaps;
	}
	
	
	@Override
	public void reset(int iteration) {
		valueMaps = new ArrayList<Map<Id,Double>>(this.breaks.size());
		countMaps = new ArrayList<Map<Id,Integer>>(this.breaks.size());
	}

	@Override
	/**
	 * 
	 */
	public void handleEvent(LinkEnterEvent event) {
		if(linkIds.contains(event.getLinkId())){
			/* The event is of interest. */
			
			/* Get the link's toll amount. */
			boolean found = false;
			int breakIndex = 0;
			while(!found && breakIndex < breaks.size()){
				if(Long.parseLong(event.getDriverId().toString()) < Long.parseLong(breaks.get(breakIndex).toString())){
					found = true;
					/* Check if the person entering the link is already contained
					 * in the map. If so, increment its toll tally. If not, add 
					 * it to the map with first toll value.
					 */
					double tollFactor = SanralTollFactor.getTollFactor(event.getDriverId(), event.getLinkId(), event.getTime());
					double toll = tollFactor * this.scheme.getLinkCostInfo(event.getLinkId(), event.getTime(), event.getDriverId(), event.getVehicleId() ).amount;
					if(valueMaps.get(breakIndex).containsKey(event.getDriverId())){
						valueMaps.get(breakIndex).put(event.getDriverId(), valueMaps.get(breakIndex).get(event.getDriverId()) + toll);
						countMaps.get(breakIndex).put(event.getDriverId(), countMaps.get(breakIndex).get(event.getDriverId()) + 1);
					} else{
						valueMaps.get(breakIndex).put(event.getDriverId(), new Double(toll));
						countMaps.get(breakIndex).put(event.getDriverId(), new Integer(1));
					}
				} else{
					breakIndex++;
				}
			}
			if(!found){
				log.warn("Could not identify a category (bracket) for agent " + event.getDriverId().toString());
			}
		}
	}

}

