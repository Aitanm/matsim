/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler4.java
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

package playground.yu.test;

import java.io.IOException;

import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;

import playground.yu.analysis.PtCheck2;

public class PtCheckTest {

	public static void main(final String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String plansFilename = "./test/yu/test/input/10pctZrhCarPt100.plans.xml.gz";
		final String ptcheckFilename = "./test/yu/test/output/ptCheck100.10pctZrhCarPt.txt";
		Gbl
				.createConfig(new String[] { "./test/yu/test/configPtcheckTest.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		try {
			PtCheck2 pc = new PtCheck2(ptcheckFilename);

			new MatsimPopulationReader(population, network)
					.readFile(plansFilename);
			pc.run(population);

			pc.write(100);
			pc.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("-->Done!!");
	}

}
