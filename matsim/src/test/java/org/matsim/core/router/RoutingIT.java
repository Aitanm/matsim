/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.LinkToLinkTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelDisutility;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TurnRestrictedLeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.testcases.MatsimTestUtils;

public class RoutingIT {
	/*package*/ static final Logger log = Logger.getLogger(RoutingIT.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculatorFactory getFactory();
	}
	
	private interface RestrictedRouterProvider {
		public String getName();
		public TurnRestrictedLeastCostPathCalculatorFactory getFactory();
	}
	
	private interface LinkToLinkRouterProvider {
		public String getName();
		public LinkToLinkLeastCostPathCalculatorFactory getFactory();
	}
	
	private RouterProvider getDijkstraProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "Dijkstra";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new DijkstraFactory();
			}
		};
	}
	
	private RouterProvider getDijkstraPruneDeadEndsProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "DijkstraPruneDeadEnds";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new DijkstraFactory(true);
			}
		};
	}
	
	private RouterProvider getAStarEuclideanProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "AStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new AStarEuclideanFactory();
			}
		};
	}
	
	private RouterProvider getAStarLandmarksProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "AStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new AStarLandmarksFactory();
			}
		};
	}
	
	private RouterProvider getFastDijkstraProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "FastDijkstra";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new FastDijkstraFactory();
			}
		};
	}
	
	private RouterProvider getFastDijkstraPruneDeadEndsProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "FastDijkstraPruneDeadEnds";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new FastDijkstraFactory(true);
			}
		};
	}
	
	private RouterProvider getFastAStarEuclideanProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new FastAStarEuclideanFactory();
			}
		};
	}
	
	private RouterProvider getFastAStarLandmarksProvider() {
		return new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory() {
				return new FastAStarLandmarksFactory();
			}
		};
	}
	
	private RestrictedRouterProvider getTurnRestrictedFastDijkstraProvider() {
		return new RestrictedRouterProvider() {
			@Override
			public String getName() {
				return "TurnRestrictedFastDijkstra";
			}
			@Override
			public TurnRestrictedLeastCostPathCalculatorFactory getFactory() {
				return new TurnRestrictedFastDijkstraFactory();
			}
		};
	}
	
	private RestrictedRouterProvider getTurnRestrictedFastDijkstraPruneDeadEndsProvider() {
		return new RestrictedRouterProvider() {
			@Override
			public String getName() {
				return "TurnRestrictedFastDijkstraPruneDeadEnds";
			}
			@Override
			public TurnRestrictedLeastCostPathCalculatorFactory getFactory() {
				return new TurnRestrictedFastDijkstraFactory(true);
			}
		};
	}
	
	private RestrictedRouterProvider getTurnRestrictedFastAStarEuclideanProvider() {
		return new RestrictedRouterProvider() {
			@Override
			public String getName() {
				return "TurnRestrictedFastAStarEuclidean";
			}
			@Override
			public TurnRestrictedLeastCostPathCalculatorFactory getFactory() {
				return new TurnRestrictedFastAStarEuclideanFactory();
			}
		};
	}
	
	private RestrictedRouterProvider getTurnRestrictedFastAStarLandmarksProvider() {
		return new RestrictedRouterProvider() {
			@Override
			public String getName() {
				return "TurnRestrictedFastAStarLandmarks";
			}
			@Override
			public TurnRestrictedLeastCostPathCalculatorFactory getFactory() {
				return new TurnRestrictedFastAStarLandmarksFactory();
			}
		};
	}
	
	private LinkToLinkRouterProvider getLinkToLinkFastDijkstraProvider() {
		return new LinkToLinkRouterProvider() {
			@Override
			public String getName() {
				return "LinkToLinkFastDijkstra";
			}
			@Override
			public LinkToLinkLeastCostPathCalculatorFactory getFactory() {
				return new LinkToLinkFastDijkstraFactory();
			}
		};
	}
	
	private LinkToLinkRouterProvider getLinkToLinkFastDijkstraPruneDeadEndsProvider() {
		return new LinkToLinkRouterProvider() {
			@Override
			public String getName() {
				return "LinkToLinkFastDijkstraPruneDeadEnds";
			}
			@Override
			public LinkToLinkLeastCostPathCalculatorFactory getFactory() {
				return new LinkToLinkFastDijkstraFactory(true);
			}
		};
	}
	
	private LinkToLinkRouterProvider getLinkToLinkFastAStarEuclideanProvider() {
		return new LinkToLinkRouterProvider() {
			@Override
			public String getName() {
				return "LinkToLinkFastAStarEuclidean";
			}
			@Override
			public LinkToLinkLeastCostPathCalculatorFactory getFactory() {
				return new LinkToLinkFastAStarEuclideanFactory();
			}
		};
	}
	
	private LinkToLinkRouterProvider getLinkToLinkFastAStarLandmarksProvider() {
		return new LinkToLinkRouterProvider() {
			@Override
			public String getName() {
				return "LinkToLinkFastAStarLandmarks";
			}
			@Override
			public LinkToLinkLeastCostPathCalculatorFactory getFactory() {
				return new LinkToLinkFastAStarLandmarksFactory();
			}
		};
	}
	
	@Test
	public void testDijkstra() {
		doTest(getDijkstraProvider());
	}
	@Test
	public void testFastDijkstra() {
		doTest(getFastDijkstraProvider());
	}
	@Test
	public void testTurnRestrictedFastDijkstra() {
		doTest(getTurnRestrictedFastDijkstraProvider());
	}
	@Test
	public void testLinkToLinkFastDijkstra() {
		doTest(getLinkToLinkFastDijkstraProvider());
	}
	@Test
	public void testDijkstraPruneDeadEnds() {
		doTest(getDijkstraPruneDeadEndsProvider());
	}
	@Test
	public void testFastDijkstraPruneDeadEnds() {
		doTest(getFastDijkstraPruneDeadEndsProvider());
	}
	@Test
	public void testTurnRestrictedFastDijkstraPruneDeadEnds() {
		doTest(getTurnRestrictedFastDijkstraPruneDeadEndsProvider());
	}
	@Test
	public void testLinkToLinkFastDijkstraPruneDeadEnds() {
		doTest(getLinkToLinkFastDijkstraPruneDeadEndsProvider());
	}
	@Test	
	public void testAStarEuclidean() {
		doTest(getAStarEuclideanProvider());
	}
	@Test
	public void testFastAStarEuclidean() {
		doTest(getFastAStarEuclideanProvider());
	}
	@Test
	public void testTurnRestrictedFastAStarEuclidean() {
		doTest(getTurnRestrictedFastAStarEuclideanProvider());
	}
	@Test
	public void testLinkToLinkFastAStarEuclidean() {
		doTest(getLinkToLinkFastAStarEuclideanProvider());
	}
	@Test	
	public void testAStarLandmarks() {
		doTest(getAStarLandmarksProvider());
	}
	@Test
	public void testFastAStarLandmarks() {
		doTest(getFastAStarLandmarksProvider());
	}
	@Test
	public void testTurnRestrictedFastAStarLandmarks() {
		doTest(getTurnRestrictedFastAStarLandmarksProvider());
	}
	@Test
	public void testLinkToLinkFastAStarLandmarks() {
		doTest(getLinkToLinkFastAStarLandmarksProvider());
	}
	@Test
	public void testTurnRestrictions() {
		
		// Fast routers
		List<RouterProvider> fastProviders = new ArrayList<>();
		fastProviders.add(getFastDijkstraProvider()); // FastDijkstra
		fastProviders.add(getFastDijkstraPruneDeadEndsProvider()); // FastDijkstraPruneDeadEnds
		fastProviders.add(getFastAStarEuclideanProvider()); // FastAStarEuclidean
		fastProviders.add(getFastAStarLandmarksProvider()); // FastAStarLandmarks
		for (RouterProvider provider : fastProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getTurnRestrictionsScenario();
			calcRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);
		}
		
		// TurnRestricted routers
		List<RestrictedRouterProvider> restrictedProviders = new ArrayList<>();
		restrictedProviders.add(getTurnRestrictedFastDijkstraProvider()); // TurnRestrictedFastDijkstra
		restrictedProviders.add(getTurnRestrictedFastDijkstraPruneDeadEndsProvider()); // TurnRestrictedFastDijkstraPruneDeadEnds
		restrictedProviders.add(getTurnRestrictedFastAStarEuclideanProvider()); // TurnRestrictedFastAStarEuclidean
		restrictedProviders.add(getTurnRestrictedFastAStarLandmarksProvider()); // TurnRestrictedFastAStarLandmarks
		for (RestrictedRouterProvider provider : restrictedProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getTurnRestrictionsScenario();
			calcRestrictedRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BC")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("CD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);
		}
		
		// LinkToLink routers
		List<LinkToLinkRouterProvider> linkToLinkProviders = new ArrayList<>();
		linkToLinkProviders.add(getLinkToLinkFastDijkstraProvider()); // LinkToLinkFastDijkstra
		linkToLinkProviders.add(getLinkToLinkFastDijkstraPruneDeadEndsProvider()); // LinkToLinkFastDijkstraPruneDeadEnds
		linkToLinkProviders.add(getLinkToLinkFastAStarEuclideanProvider()); // LinkToLinkFastAStarEuclidean
		linkToLinkProviders.add(getLinkToLinkFastAStarLandmarksProvider()); // LinkToLinkFastAStarLandmarks
		for (LinkToLinkRouterProvider provider : linkToLinkProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getTurnRestrictionsScenario();
			calcLinkToLinkRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BC")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("CD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);
		}
	}
	
	@Test
	public void testMultipleToNodes() {
		
		// TurnRestricted routers
		List<RestrictedRouterProvider> restrictedProviders = new ArrayList<>();
		restrictedProviders.add(getTurnRestrictedFastDijkstraProvider()); // TurnRestrictedFastDijkstra
		restrictedProviders.add(getTurnRestrictedFastDijkstraPruneDeadEndsProvider()); // TurnRestrictedFastDijkstraPruneDeadEnds
		restrictedProviders.add(getTurnRestrictedFastAStarEuclideanProvider()); // TurnRestrictedFastAStarEuclidean
		restrictedProviders.add(getTurnRestrictedFastAStarLandmarksProvider()); // TurnRestrictedFastAStarLandmarks
		for (RestrictedRouterProvider provider : restrictedProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getMultipleToNodesScenario();
			calcRestrictedRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);			
		}
		
		// LinkToLink routers
		List<LinkToLinkRouterProvider> linkToLinkProviders = new ArrayList<>();
		linkToLinkProviders.add(getLinkToLinkFastDijkstraProvider()); // LinkToLinkFastDijkstra
		linkToLinkProviders.add(getLinkToLinkFastDijkstraPruneDeadEndsProvider()); // LinkToLinkFastDijkstraPruneDeadEnds
		linkToLinkProviders.add(getLinkToLinkFastAStarEuclideanProvider()); // LinkToLinkFastAStarEuclidean
		linkToLinkProviders.add(getLinkToLinkFastAStarLandmarksProvider()); // LinkToLinkFastAStarLandmarks
		for (LinkToLinkRouterProvider provider : linkToLinkProviders) {
			log.info("testing " + provider.getName());			
			final Scenario scenario = getMultipleToNodesScenario();		
			calcLinkToLinkRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);
		}
	}

	/*
	 * The regular Dijkstra implementation does not support detours to avoid turn restrictions. Test whether our implementation does... 
	 */
	@Test
	public void testDetourDueToTurnRestrictions() {
		
		// TurnRestricted routers
		List<RestrictedRouterProvider> restrictedProviders = new ArrayList<>();
		restrictedProviders.add(getTurnRestrictedFastDijkstraProvider()); // TurnRestrictedFastDijkstra
		restrictedProviders.add(getTurnRestrictedFastDijkstraPruneDeadEndsProvider()); // TurnRestrictedFastDijkstraPruneDeadEnds
		restrictedProviders.add(getTurnRestrictedFastAStarEuclideanProvider()); // TurnRestrictedFastAStarEuclidean
		restrictedProviders.add(getTurnRestrictedFastAStarLandmarksProvider()); // TurnRestrictedFastAStarLandmarks
		for (RestrictedRouterProvider provider : restrictedProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getTurnRestrictionsDetourScenario();
			calcRestrictedRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DC")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("CD")).getLength() +
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);			
		}
				
		// LinkToLink routers
		List<LinkToLinkRouterProvider> linkToLinkProviders = new ArrayList<>();
		linkToLinkProviders.add(getLinkToLinkFastDijkstraProvider()); // LinkToLinkFastDijkstra
		linkToLinkProviders.add(getLinkToLinkFastDijkstraPruneDeadEndsProvider()); // TurnRestrictedFastDijkstraPruneDeadEnds
		linkToLinkProviders.add(getLinkToLinkFastAStarEuclideanProvider()); // LinkToLinkFastAStarEuclidean
		linkToLinkProviders.add(getLinkToLinkFastAStarLandmarksProvider()); // LinkToLinkFastAStarLandmarks
		for (LinkToLinkRouterProvider provider : linkToLinkProviders) {
			log.info("testing " + provider.getName());
			final Scenario scenario = getTurnRestrictionsDetourScenario();
			calcLinkToLinkRoute(provider, scenario);
			
			// at the moment, the length of the from-link is ignored but the length of the to-link is taken into account
			double expected = scenario.getNetwork().getLinks().get(Id.createLinkId("BD")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("DC")).getLength() + 
					scenario.getNetwork().getLinks().get(Id.createLinkId("CD")).getLength() +
					scenario.getNetwork().getLinks().get(Id.createLinkId("DE")).getLength();
			
			Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId("P")).getSelectedPlan();
			
			Assert.assertEquals(expected, ((Leg) plan.getPlanElements().get(1)).getRoute().getDistance(), 0.0);
		}
	}
	
	private void doTest(final RouterProvider provider) {
//		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Config config = ConfigUtils.loadConfig(this.utils.getClassInputDirectory() + "/config.xml" );
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		final String inPlansName = this.utils.getClassInputDirectory() + "/plans.xml.gz" ;
		new PopulationReader(scenario).readFile(inPlansName);
			
		calcRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario.getNetwork()).readFile(config.network().getInputFile());
		new PopulationReader(referenceScenario).readFile(inPlansName);
		
		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		if (!isEqual) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assert.assertTrue("different plans files.", isEqual);
	}

	private void doTest(final RestrictedRouterProvider provider) {
//		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Config config = ConfigUtils.loadConfig(this.utils.getClassInputDirectory() + "/config.xml" );
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		final String inPlansName = this.utils.getClassInputDirectory() + "/plans.xml.gz" ;
		new PopulationReader(scenario).readFile(inPlansName);
			
		calcRestrictedRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario.getNetwork()).readFile(config.network().getInputFile());
		new PopulationReader(referenceScenario).readFile(inPlansName);
		
		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		if (!isEqual) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assert.assertTrue("different plans files.", isEqual);
	}
	
	private void doTest(final LinkToLinkRouterProvider provider) {
//		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Config config = ConfigUtils.loadConfig(this.utils.getClassInputDirectory() + "/config.xml" );
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		final String inPlansName = this.utils.getClassInputDirectory() + "/plans.xml.gz" ;
		new PopulationReader(scenario).readFile(inPlansName);
			
		calcLinkToLinkRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario.getNetwork()).readFile(config.network().getInputFile());
		new PopulationReader(referenceScenario).readFile(inPlansName);
		
		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		if (!isEqual) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assert.assertTrue("different plans files.", isEqual);
	}
	
	/*
	 * Use a network like:
	 * 
	 *      [C]
	 *       | \
	 *       |  \
	 *       |   \      
	 * [A]--[B]--[D]--[E]
	 * 
	 * B -> D is not permitted due to turn restrictions when arriving from A.
	 */
	private Scenario getTurnRestrictionsScenario() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		Population population = scenario.getPopulation();
		
		NetworkFactory networkFactory = network.getFactory();
		LanesFactory lanesFactory = lanes.getFactory();
		PopulationFactory populationFactory = population.getFactory();
		
		Node a = networkFactory.createNode(Id.createNodeId("A"), new Coord(0, 0));
		Node b = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000, 0));
		Node c = networkFactory.createNode(Id.createNodeId("C"), new Coord(1000, 1000));
		Node d = networkFactory.createNode(Id.createNodeId("D"), new Coord(2000, 0));
		Node e = networkFactory.createNode(Id.createNodeId("E"), new Coord(3000, 0));
		
		Link ab = networkFactory.createLink(Id.createLinkId("AB"), a, b);
		Link bc = networkFactory.createLink(Id.createLinkId("BC"), b, c);
		Link bd = networkFactory.createLink(Id.createLinkId("BD"), b, d);
		Link cd = networkFactory.createLink(Id.createLinkId("CD"), c, d);
		Link de = networkFactory.createLink(Id.createLinkId("DE"), d, e);
		
		ab.setLength(1000);
		bc.setLength(1000);
		bd.setLength(1000);
		cd.setLength(2000);
		de.setLength(1000);
		
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);
		network.addNode(e);
		
		network.addLink(ab);
		network.addLink(bc);
		network.addLink(bd);
		network.addLink(cd);
		network.addLink(de);
		
		Lane lane = lanesFactory.createLane(Id.create("AB", Lane.class));
		lane.addToLinkId(Id.createLinkId("BC"));	// allow B -> C but not B -> D
		LanesToLinkAssignment assignment = lanesFactory.createLanesToLinkAssignment(Id.createLinkId("AB"));
		lanes.addLanesToLinkAssignment(assignment);
		assignment.addLane(lane);
		
		Person person = populationFactory.createPerson(Id.createPersonId("P"));
		population.addPerson(person);
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		Activity home = populationFactory.createActivityFromCoord("home", new Coord(0, 0));
		home.setEndTime(8 * 3600);
		home.setLinkId(Id.createLinkId("AB"));
		plan.addActivity(home);
		plan.addLeg(populationFactory.createLeg(TransportMode.car));
		Activity work = populationFactory.createActivityFromCoord("work", new Coord(3000, 0));
		work.setLinkId(Id.createLinkId("DE"));
		plan.addActivity(work);
		
		return scenario;
	}
	
	/*
	 * Use a network like:
	 * 
	 *      [C]  [G]
	 *       | \  |
	 *       |  \ |
	 *       |   \|      
	 * [A]--[B]--[D]--[E]
	 *            |
	 *            |
	 *           [F]
	 * 
	 * D -> F is not permitted due to turn restrictions when arriving from B.
	 * When arriving from C, the node is unrestricted.
	 * D -> G is not permitted due to turn restrictions when arriving from C.
	 * 
	 * As a result, D is duplicated and the router has to take both into account.
	 * Moreover, E can only be arrived from duplicated versions of D, i.e. we 
	 * check whether the algorithm really checks the array of valid to-nodes!
	 */
	private Scenario getMultipleToNodesScenario() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		Population population = scenario.getPopulation();
		
		NetworkFactory networkFactory = network.getFactory();
		LanesFactory lanesFactory = lanes.getFactory();
		PopulationFactory populationFactory = population.getFactory();
		
		Node a = networkFactory.createNode(Id.createNodeId("A"), new Coord(0, 0));
		Node b = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000, 0));
		Node c = networkFactory.createNode(Id.createNodeId("C"), new Coord(1000, 1000));
		Node d = networkFactory.createNode(Id.createNodeId("D"), new Coord(2000, 0));
		Node e = networkFactory.createNode(Id.createNodeId("E"), new Coord(3000, 0));
		Node f = networkFactory.createNode(Id.createNodeId("F"), new Coord(2000, -1000));
		Node g = networkFactory.createNode(Id.createNodeId("G"), new Coord(2000, 1000));
		
		Link ab = networkFactory.createLink(Id.createLinkId("AB"), a, b);
		Link bc = networkFactory.createLink(Id.createLinkId("BC"), b, c);
		Link bd = networkFactory.createLink(Id.createLinkId("BD"), b, d);
		Link cd = networkFactory.createLink(Id.createLinkId("CD"), c, d);
		Link de = networkFactory.createLink(Id.createLinkId("DE"), d, e);
		Link df = networkFactory.createLink(Id.createLinkId("DF"), d, f);
		Link dg = networkFactory.createLink(Id.createLinkId("DG"), d, g);
		
		ab.setLength(1000);
		bc.setLength(1000);
		bd.setLength(1000);
		cd.setLength(2000);
		de.setLength(1000);
		df.setLength(1000);
		dg.setLength(1000);
		
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);
		network.addNode(e);
		network.addNode(f);
		network.addNode(g);
		
		network.addLink(ab);
		network.addLink(bc);
		network.addLink(bd);
		network.addLink(cd);
		network.addLink(de);
		network.addLink(df);
		network.addLink(dg);
		
		Lane lane;
		LanesToLinkAssignment assignment;
		
		lane = lanesFactory.createLane(Id.create("BD", Lane.class));
		lane.addToLinkId(Id.createLinkId("DE"));	// allow D -> E but not D -> G
		lane.addToLinkId(Id.createLinkId("DF"));	// allow D -> F but not D -> G
		assignment = lanesFactory.createLanesToLinkAssignment(Id.createLinkId("BD"));
		lanes.addLanesToLinkAssignment(assignment);
		assignment.addLane(lane);
		
		lane = lanesFactory.createLane(Id.create("CD", Lane.class));
		lane.addToLinkId(Id.createLinkId("DE"));	// allow D -> E but not D -> F
		lane.addToLinkId(Id.createLinkId("DG"));	// allow D -> G but not D -> G
		assignment = lanesFactory.createLanesToLinkAssignment(Id.createLinkId("CD"));
		lanes.addLanesToLinkAssignment(assignment);
		assignment.addLane(lane);
		
		Person person = populationFactory.createPerson(Id.createPersonId("P"));
		population.addPerson(person);
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		Activity home = populationFactory.createActivityFromCoord("home", new Coord(0, 0));
		home.setEndTime(8 * 3600);
		home.setLinkId(Id.createLinkId("AB"));
		plan.addActivity(home);
		plan.addLeg(populationFactory.createLeg(TransportMode.car));
		Activity work = populationFactory.createActivityFromCoord("work", new Coord(3000, 0));
		work.setLinkId(Id.createLinkId("DE"));
		plan.addActivity(work);
		
		return scenario;
	}
	
	/*
	 * Model a scenario where a person has to accept a detour to reach the destination.
	 * Problem: The router must realize that a node has to be reached from another link with higher costs to go on.
	 * Example: The agent starts at A and wants to go to E. However, at D, only right turns are allowed, i.e. the
	 * route has to be A -> B -> D -> C -> D -> E.
	 * 
	 * We also test whether the Router ensures that the toLink can be accessed from the previous link. In case
	 * we search a route from AB to DE, the router routes from B to D. Then, the algorithm has to check whether
	 * DE can be accessed directly from BD. If not, a detour has to be made! 
	 * 
	 *         [A]
	 *          |
	 *          |
	 *         [B]
	 *          |
	 *          |
	 *   [C]---[D]---[E]
	 *          |
	 *          |
	 *         [F]
	 */
	private Scenario getTurnRestrictionsDetourScenario() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		Population population = scenario.getPopulation();
		
		NetworkFactory networkFactory = network.getFactory();
		LanesFactory lanesFactory = lanes.getFactory();
		PopulationFactory populationFactory = population.getFactory();
		
		Node a = networkFactory.createNode(Id.createNodeId("A"), new Coord(1000, 2000));
		Node b = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000, 1000));
		Node c = networkFactory.createNode(Id.createNodeId("C"), new Coord(0, 0));
		Node d = networkFactory.createNode(Id.createNodeId("D"), new Coord(1000, 0));
		Node e = networkFactory.createNode(Id.createNodeId("E"), new Coord(2000, 0));
		Node f = networkFactory.createNode(Id.createNodeId("F"), new Coord(1000, -1000));
		
		Link ab = networkFactory.createLink(Id.createLinkId("AB"), a, b);
		Link bd = networkFactory.createLink(Id.createLinkId("BD"), b, d);
		Link cd = networkFactory.createLink(Id.createLinkId("CD"), c, d);
		Link dc = networkFactory.createLink(Id.createLinkId("DC"), d, c);
		Link de = networkFactory.createLink(Id.createLinkId("DE"), d, e);
		Link df = networkFactory.createLink(Id.createLinkId("DF"), d, f);
		
		ab.setLength(1000);
		bd.setLength(1000);
		cd.setLength(1000);
		dc.setLength(1000);
		de.setLength(1000);
		df.setLength(1000);
		
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);
		network.addNode(e);
		network.addNode(f);
		
		network.addLink(ab);
		network.addLink(bd);
		network.addLink(cd);
		network.addLink(dc);
		network.addLink(de);
		network.addLink(df);
		
		/*
		 * Force router to find a detour since turning left ist not allowed at [D] when
		 * arriving from [BD].
		 */
		Lane lane = lanesFactory.createLane(Id.create("BD", Lane.class));
		lane.addToLinkId(Id.createLinkId("DC"));	// allow D -> C but not D -> E
		LanesToLinkAssignment assignment = lanesFactory.createLanesToLinkAssignment(Id.createLinkId("BD"));
		lanes.addLanesToLinkAssignment(assignment);
		assignment.addLane(lane);
		
		/*
		 * This restriction makes node [D] a restricted node when reached from [C], i.e. we can check
		 * whether the router realizes this and checks all valid versions of [D] in the routing network.
		 */
		lane = lanesFactory.createLane(Id.create("CD", Lane.class));
		lane.addToLinkId(Id.createLinkId("DE"));	// allow D -> E but not D -> F
		assignment = lanesFactory.createLanesToLinkAssignment(Id.createLinkId("CD"));
		lanes.addLanesToLinkAssignment(assignment);
		assignment.addLane(lane);
		
		Person person = populationFactory.createPerson(Id.createPersonId("P"));
		population.addPerson(person);
		
		Plan plan = populationFactory.createPlan();
		person.addPlan(plan);
		Activity home = populationFactory.createActivityFromCoord("home", new Coord(0, 0));
		home.setEndTime(8 * 3600);
		home.setLinkId(Id.createLinkId("AB"));
		plan.addActivity(home);
		plan.addLeg(populationFactory.createLeg(TransportMode.car));
		Activity work = populationFactory.createActivityFromCoord("work", new Coord(3000, 0));
		work.setLinkId(Id.createLinkId("DE"));
		plan.addActivity(work);
		
		return scenario;
	}
	
	private static void calcRoute(final RouterProvider provider, final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());

		final FreespeedTravelTimeAndDisutility calculator = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		final LeastCostPathCalculatorFactory factory1 = provider.getFactory();
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						install(new ScenarioByInstanceModule(scenario));
						addTravelTimeBinding("car").toInstance(calculator);
						addTravelDisutilityFactoryBinding("car").toInstance(new TravelDisutilityFactory() {
							@Override
							public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
								return calculator;
							}
						});
						bindLeastCostPathCalculatorFactory().toInstance(factory1);
					}
				}));
			}
		});

		final TripRouter tripRouter = injector.getInstance(TripRouter.class);
		final PersonAlgorithm router = new PlanRouter(tripRouter);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			router.run(p);
		}
	}
	
	private static void calcRestrictedRoute(final RestrictedRouterProvider provider, final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());

		final FreespeedTravelTimeAndDisutility calculator = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		final TravelDisutilityFactory travelDisutilityFactory = new TravelDisutilityFactory() {		
			@Override
			public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
				return calculator;
			}
		};
		
		TurnRestrictedRoutingModule routingModule = new TurnRestrictedRoutingModule("car", scenario.getPopulation().getFactory(), scenario.getNetwork(),
				scenario.getLanes(), provider.getFactory(), travelDisutilityFactory, calculator);

		TripRouter.Builder tripRouterBuilder = new TripRouter.Builder(scenario.getConfig());
		tripRouterBuilder.setRoutingModule("car", routingModule);
		final TripRouter tripRouter = tripRouterBuilder.build();
		final PersonAlgorithm router = new PlanRouter(tripRouter);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			router.run(p);
		}
	}
	
	private static void calcLinkToLinkRoute(final LinkToLinkRouterProvider provider, final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());

		final FreespeedTravelTimeAndDisutility calculator = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		final LinkToLinkTravelDisutilityFactory travelDisutilityFactory = new LinkToLinkTravelDisutilityFactory() {		
			@Override
			public LinkToLinkTravelDisutility createLinkToLinkTravelDisutility(LinkToLinkTravelTime timeCalculator) {
				return calculator;
			}
		};
		
		LinkToLinkRoutingModuleV2 routingModule = new LinkToLinkRoutingModuleV2("car", scenario.getPopulation().getFactory(), scenario.getNetwork(),
				scenario.getLanes(), provider.getFactory(), travelDisutilityFactory, calculator);

		TripRouter.Builder tripRouterBuilder = new TripRouter.Builder(scenario.getConfig());
		tripRouterBuilder.setRoutingModule("car", routingModule);
		final TripRouter tripRouter = tripRouterBuilder.build();
		final PersonAlgorithm router = new PlanRouter(tripRouter);
		
		for (Person p : scenario.getPopulation().getPersons().values()) {
			router.run(p);
		}
	}
}