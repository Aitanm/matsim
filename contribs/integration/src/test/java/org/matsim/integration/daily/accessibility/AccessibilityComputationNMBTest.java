/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.integration.daily.accessibility;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityStartupListener;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtModule;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dziemke
 */
public class AccessibilityComputationNMBTest {
	public static final Logger log = Logger.getLogger( AccessibilityComputationNMBTest.class ) ;

	private static final Double cellSize = 500.;
//	private static final double time = 8.*60*60;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	public void doAccessibilityTest() throws IOException {
		// Input
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";

		/* Adapt folder structure that may be different on different machines, especially on server */
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		
		networkFile = folderStructure + networkFile ;
		String facilitiesFile = folderStructure + "matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz";
		
		// Regular pt
		String travelTimeMatrixFilePT = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
		String travelDistanceMatrixFilePT = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
		String ptStopsFilePT = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";
				
		// Minibus pt
//		String travelTimeMatrixFileParatransit = folderStructure + "matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/travelTimeMatrix_0.csv.gz";
//		String travelDistanceMatrixFileParatransit = folderStructure + "matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/travelDistanceMatrix_0.csv.gz";
//		String ptStopsFileParatransit = folderStructure + "matsimExamples/countries/za/nmb/minibus-pt/jtlu14b/matrix_grid_1000/ptStops.csv";

		// Parameters
		String crs = TransformationFactory.WGS84_SA_Albers;
		String layerName = "za_nmb_" + cellSize.toString().split("\\.")[0];
		
		//QGis
		boolean createQGisOutput = true;
		boolean includeDensityLayer = true;
		Double lowerBound = -3.5;
		Double upperBound = 3.5;
		Integer range = 9; // in the current implementation, this must always be 9
		int symbolSize = 525;
		int populationThreshold = (int) (120 / (1000/cellSize * 1000/cellSize));

		/* Extent of the network are (as they can looked up by using the bounding box):
		/* minX = 111083.9441831379, maxX = 171098.03695045778, minY = -3715412.097693177,	maxY = -3668275.43481496 */
//		double[] mapViewExtent = {100000,-3720000,180000,-3675000}; // choose map view a bit bigger
		double[] mapViewExtent = {115000,-3718000,161000,-3679000};

		// Config and scenario
		Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.abort);

		// Some (otherwise irrelevant) settings to make the vsp check happy:
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings(stratSets);
		
		final Scenario scenario = ScenarioUtils.loadScenario( config );
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		// yy For a test, "abort" may be too strict.  kai, may'16
		
		// matrix-based pt
		MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule(MatrixBasedPtRouterConfigGroup.GROUP_NAME);
		mbpcg.setPtStopsInputFile(ptStopsFilePT);
		mbpcg.setUsingTravelTimesAndDistances(true);
		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFilePT);
		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFilePT);

		// plansClacRoute parameters
		PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();

		// if no travel matrix (distances and times) is provided, the teleported mode speed for pt needs to be set
		// teleported mode speed for pt also required, see PtMatrix:120
//      ModeRoutingParams ptParameters = new ModeRoutingParams(TransportMode.pt);
//      ptParameters.setTeleportedModeSpeed(50./3.6);
//      plansCalcRoute.addModeRoutingParams(ptParameters);

		// by adding ModeRoutingParams (as done above for pt), the other parameters are deleted
		// the walk and bike parameters are needed, however. This is why they have to be set here again
        
		// teleported mode speed for walking also required, see PtMatrix:141
		ModeRoutingParams walkParameters = new ModeRoutingParams(TransportMode.walk);
		walkParameters.setTeleportedModeSpeed(3./3.6);
		plansCalcRoute.addModeRoutingParams(walkParameters );

		// teleported mode speed for bike also required, see AccessibilityControlerListenerImpl:168
		ModeRoutingParams bikeParameters = new ModeRoutingParams(TransportMode.bike);
		bikeParameters.setTeleportedModeSpeed(15./3.6);
		plansCalcRoute.addModeRoutingParams(bikeParameters );
		
		// pt matrix
//      BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
//		PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, boundingBox, mbpcg);

		assertNotNull(config);

		
		// collect activity types
//		List<String> activityTypes = AccessibilityRunUtils.collectAllFacilityTypes(scenario);
//		log.warn( "found activity types: " + activityTypes );
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some
		// other algorithms, the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		List<String> activityTypes = new ArrayList<String>();
		activityTypes.add("s");
		
		// collect homes
		String activityFacilityType = "h";
		ActivityFacilities homes = AccessibilityRunUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);
		
		
		
		
		AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
//		accessibilityConfigGroup.setComputingAccessibilityForMode(Modes4Accessibility.paratransit, true);
		


//		Map<String, ActivityFacilities> activityFacilitiesMap = new HashMap<String, ActivityFacilities>();
		
		
		Controler controler = new Controler(scenario) ;

//		final GeoserverUpdater geoserverUpdater = new GeoserverUpdater(crs, name);
//		geoserverUpdater.addAdditionalFacilityData(homes) ; 

//		List<Modes4Accessibility> modes = new ArrayList<>() ;
//		modes.add( Modes4Accessibility.freeSpeed ) ;
//		modes.add( Modes4Accessibility.car ) ;
//		modes.add( Modes4Accessibility.walk ) ;
//		modes.add( Modes4Accessibility.bike ) ;
//		modes.add( Modes4Accessibility.pt ) ;
		
		controler.addControlerListener(new AccessibilityStartupListener(activityTypes, homes, crs, layerName, cellSize));
		controler.addOverridingModule(new MatrixBasedPtModule());
		controler.run();
		
//		geoserverUpdater.setAndProcessSpatialGrids(modes);

		
//		if (createQGisOutput == true) {
//			String osName = System.getProperty("os.name");
//			String workingDirectory = config.controler().getOutputDirectory();
//
//			for (String actType : activityTypes) {
//				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
//
//				for ( Modes4Accessibility mode : Modes4Accessibility.values()) {
//					if ( !actType.equals("s") ) {
//						log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//						continue ;
//					}
//					VisualizationUtils.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
//							lowerBound, upperBound, range, symbolSize, populationThreshold);
//					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
//				}
//			}  
//		}
	}
}