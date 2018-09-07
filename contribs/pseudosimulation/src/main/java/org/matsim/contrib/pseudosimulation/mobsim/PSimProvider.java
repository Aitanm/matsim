/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author fouriep
 */
public class PSimProvider implements Provider<Mobsim> {

	@Inject
	private PlanCatcher plans;
	@Inject
	private TravelTime travelTime;
	@Inject private WaitTime waitTime;
	@Inject private StopStopTime stopStopTime;
	// private TransitPerformance transitPerformance;
	private final Scenario scenario;
	private final EventsManager eventsManager;

	@Inject
	public PSimProvider(Scenario scenario, EventsManager eventsManager) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
	}

	@Override
	public Mobsim get() {
		// if (iteration > 0)
		// eventsManager.resetHandlers(iteration++);
		// else
		// iteration++;
		// if (waitTime != null) {
//		 return new PSim(scenario, eventsManager, plans.getPlansForPSim(), travelTime,
//		 waitTime, stopStopTime, transitPerformance);
		//
		// } else {
		// return new PSim(scenario, eventsManager, plans.getPlansForPSim(), travelTime);
		// }
//		 return new PSim(scenario, eventsManager, plans.getPlansForPSim(), travelTime,
//		 null, null, transitPerformance);	
		
		Logger.getLogger(PSimProvider.class).info("number of plans: " + plans.getPlansForPSim().size());
		
		 return new PSim(scenario, eventsManager, plans.getPlansForPSim(), travelTime,
		 waitTime, stopStopTime, null);
	}

	public void setTravelTime(TravelTime travelTime) {
		// this.travelTime = travelTime;
		throw new UnsupportedOperationException();
	}

	public void setWaitTime(WaitTime waitTime) {
		// this.waitTime = waitTime;
		throw new UnsupportedOperationException();
	}

	public void setStopStopTime(StopStopTime stopStopTime) {
		// this.stopStopTime = stopStopTime;
		throw new UnsupportedOperationException();
	}

	public void setTransitPerformance(TransitPerformance transitPerformance) {
		// this.transitPerformance = transitPerformance;
		throw new UnsupportedOperationException();
	}

	// public void setTimes(TravelTime travelTime, WaitTime waitTime, StopStopTime
	// stopStopTime) {
	// this.travelTime = travelTime;
	// this.waitTime = waitTime;
	// this.stopStopTime = stopStopTime;
	// }

	@Deprecated
	// will replace where necessary
	public Mobsim createMobsim(Scenario scenario, EventsManager events) {
		return get();
	}
}
