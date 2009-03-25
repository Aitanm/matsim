/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAssignmentInitialiser.java
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
package playground.mfeil;

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.*;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.population.algorithms.PlanAlgorithm;
import java.util.LinkedList;



/**
 * @author Matthias Feil
 * Initializes the agentsAssigner.
 */

public class AgentsAssignmentInitialiser extends MultithreadedModuleA{
	
	
	protected final NetworkLayer 				network;
	protected final Controler					controler;
	protected final PreProcessLandmarks		preProcessRoutingData;
	protected final LocationMutatorwChoiceSet locator;
	protected final PlanScorer 				scorer;
	protected final ScheduleCleaner				cleaner;
	protected final RecyclingModule			module;
	protected final double					minimumTime;
	protected LinkedList<String>				nonassignedAgents;

		
	public AgentsAssignmentInitialiser (final Controler controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LocationMutatorwChoiceSet locator,
			final PlanScorer scorer,
			final ScheduleCleaner cleaner,
			final RecyclingModule module, 
			final double minimumTime,
			LinkedList<String> nonassignedAgents) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.preProcessRoutingData = preProcessRoutingData;	
		this.locator = locator;
		this.scorer = scorer;
		this.cleaner = cleaner;
		this.module = module;
		this.minimumTime = minimumTime;
		this.nonassignedAgents = nonassignedAgents;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm agentsAssigner;
		
		agentsAssigner = new AgentsAssigner (this.controler, this.preProcessRoutingData, 
					this.locator, this.scorer, this.cleaner, this.module, this.minimumTime, this.nonassignedAgents);
		
		return agentsAssigner;
	}
}
