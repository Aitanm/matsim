/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicLeg.Mode;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PersonAlgorithm;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.utils.geometry.CoordUtils;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 3 plans, with type
 * "car", "pt" and "walk", whose leg mode will be "pt" or "walk" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentWalkPlan extends NewPopulation implements PersonAlgorithm {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentWalkPlan(final Population plans) {
		super(plans);
	}

	public NewAgentWalkPlan(final Population population, final String filename) {
		super(population, filename);
	}

	@SuppressWarnings( { "deprecation", "unchecked" })
	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			List<Plan> copyPlans = new ArrayList<Plan>();
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				if (hasLongLegs(pl))
					break;
				// set plan type for car, pt, walk
				if (PlanModeJudger.usePt(pl)) {
					Plan walkPlan = new org.matsim.population.PlanImpl(person);
					walkPlan.setType(Plan.Type.WALK);
					List actsLegs = pl.getPlanElements();
					for (int i = 0; i < actsLegs.size(); i++) {
						Object o = actsLegs.get(i);
						if (i % 2 == 0) {
							walkPlan.addAct((Activity) o);
						} else {
							Leg leg = (Leg) o;
							// -----------------------------------------------
							// WITHOUT routeSetting!
							// -----------------------------------------------
							Leg walkLeg = new org.matsim.population.LegImpl(
									Mode.walk);
							walkLeg.setDepartureTime(leg.getDepartureTime());
							walkLeg.setTravelTime(leg.getTravelTime());
							walkLeg.setArrivalTime(leg.getArrivalTime());
							walkLeg.setRoute(null);
							walkPlan.addLeg(walkLeg);
							// if (!leg.getMode().equals(Mode.car)) {
							// leg.setRoute(null);
							// leg.setMode(Mode.car);
							// }
						}
					}
					copyPlans.add(walkPlan);
				}
			}
			for (Plan copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
		}
		this.pw.writePerson(person);
	}

	private boolean hasLongLegs(Plan plan) {
		for (LegIterator li = plan.getIteratorLeg(); li.hasNext();) {
			Leg leg = (Leg) li.next();
			if (CoordUtils.calcDistance(plan.getPreviousActivity(leg).getLink().getCoord(), plan.getNextActivity(leg).getLink().getCoord()) / 1000.0 > 3.0)
				return true;
		}
		return false;
	}

	public static void main(final String[] args) {
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		Population population = new PopulationImpl();
		NewAgentWalkPlan nawp = new NewAgentWalkPlan(population);
		new MatsimPopulationReader(population, network).readFile(config.plans()
				.getInputFile());
		nawp.run(population);
		nawp.writeEndPlans();
	}
}
