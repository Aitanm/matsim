/* *********************************************************************** *
 * project: org.matsim.*
 * CreateKnownNodesMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.ActIterator;

import playground.christoph.router.util.DeadEndRemover;

public class CreateKnownNodesMap {

	public static boolean removeDeadEnds = true;
	
	private final static Logger log = Logger.getLogger(CreateKnownNodesMap.class);
	
	public static void collectAllSelectedNodes(Population population)
	{
		for (Person person : population.getPersons().values()) 
		{
			collectAllSelectedNodes(person);
		}
	}
	
	public static void collectAllSelectedNodes(Person person)
	{		
		if(person.getKnowledge() == null) person.createKnowledge("activityroom");
			
		Plan plan = person.getSelectedPlan();
					
		Map<Id, Node> nodesMap = new TreeMap<Id, Node>();
			
		ArrayList<SelectNodes> personNodeSelectors = (ArrayList<SelectNodes>)person.getCustomAttributes().get("NodeSelectors");
		
		// for all node selectors of the person
		for (SelectNodes nodeSelector : personNodeSelectors)
		{
			collectSelectedNodes(person, nodeSelector);
		}
		// if Flag is set, remove Dead Ends from the Person's Activity Room
		if(removeDeadEnds) DeadEndRemover.removeDeadEnds(person);
	}
	
	/*
	 * The handling of the nodeSelectors should probably be outsourced...
	 * Implementing them direct in the nodeSelectors could be a good solution...
	 */
	public static void collectSelectedNodes(Person p, SelectNodes nodeSelector)
	{	
		if(p.getKnowledge() == null) p.createKnowledge("activityroom");
		
		Plan plan = p.getSelectedPlan();
		
		// get Nodes from the Person's Knowledge
		Map<Id, Node> nodesMap = (Map<Id, Node>)p.getCustomAttributes().get("Nodes");
				
		if (nodesMap == null) 
		{
			nodesMap = new TreeMap<Id, Node>();

			// add the new created Nodes to the knowledge of the person
			Map<String,Object> customKnowledgeAttributes = p.getKnowledge().getCustomAttributes();
			customKnowledgeAttributes.put("Nodes", nodesMap);
		}
		
		if(nodeSelector instanceof SelectNodesDijkstra)
		{			
			ActIterator actIterator = plan.getIteratorAct();
				
			// get all acts of the selected plan
			ArrayList<Activity> acts = new ArrayList<Activity>();					
			while(actIterator.hasNext()) acts.add((Activity)actIterator.next());
			
			for(int j = 1; j < acts.size(); j++)
			{						
				Node startNode = acts.get(j-1).getLink().getToNode();
				Node endNode = acts.get(j).getLink().getFromNode();
					
				((SelectNodesDijkstra)nodeSelector).setStartNode(startNode);
				((SelectNodesDijkstra)nodeSelector).setEndNode(endNode);

				nodeSelector.addNodesToMap(nodesMap);
			}
		}	//if instanceof SelectNodesDijkstra
			
		else if(nodeSelector instanceof SelectNodesCircular)
		{
			// do something else here...
		}
			
		else
		{
			log.error("Unkown NodeSelector!");
		}
	
	}
	
	public static void setRemoveDeadEnds(boolean value)
	{
		removeDeadEnds = value; 
	}
	
	public static boolean getRemoveDeadEnds()
	{
		return removeDeadEnds;
	}
}