package playground.wrashid.PHEV.Utility;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.ActEndEvent;
import org.matsim.core.events.ActStartEvent;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.PersonEvent;
import org.matsim.core.events.handler.ActEndEventHandler;
import org.matsim.core.events.handler.ActStartEventHandler;
import org.matsim.core.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;

//TODO: write tests for this class

public class ElectricCostHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, ActStartEventHandler, ActEndEventHandler,
		AgentMoneyEventHandler {
	// key: agentId
	// value: energyState
	private HashMap<String, EnergyApplicatonSpecificState> energyLevel = new HashMap<String, EnergyApplicatonSpecificState>();
	private final double fullEnergyLevel = 36000000; // in [J]
	// default: 36000000 (=10 kWh)
	private final double penaltyForRunningOutOfElectricEnergy = -100000000;
	private MobSimController controler = null;
	private EnergyConsumptionSamples energyConsumptionSamples = null;
	private Events events = null;

	// application specific
	private double averageTimeSpentAtWork = 0;
	private double averageTimeSpentAtShop = 0;
	private String observedVehicleId;
	// the state of charge of the observed vehicle
	// c1: time
	// c2: state of charge
	private TwoColumnTable<Double, Double> recordedStateOfCharge;
	private DESController controler2;

	// for measuring the energy consumption for a link
	private String selectedLink = "104";
	private double energyConsumptionAtLink[] = new double[130000];

	// write charging events to file
	// String outputFilePath=null;
	String outputFilePath = "C:\\data\\tempOutput\\chargingEvents.txt";
	OutputStreamWriter chargingOutput;
	
	// for example, we can say in reset, which Iteration is relevant
	// then only for that iteration the energy charging at a link will be recorded
	// also used for state of charge
	private boolean relevantIterationReached=false;

	private void initOutputFile() {
		if (outputFilePath == null) {
			return;
		}
		try {
			FileOutputStream fos = new FileOutputStream(outputFilePath);
			chargingOutput = new OutputStreamWriter(fos, "UTF8");
			chargingOutput.write("linkId\tagentId\tstartChargingTime\tendChargingTime\tSOC_start\tSOC_end\n"); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// observedVehicleId: if not null, the SOC of this vehicle is recorded
	public ElectricCostHandler(MobSimController controler,
			EnergyConsumptionSamples energyConsumptionSamples, Events events,
			String observedVehicleId) {
		this.controler = controler;
		this.energyConsumptionSamples = energyConsumptionSamples;
		this.events = events;
		this.observedVehicleId = observedVehicleId;
		if (observedVehicleId != null) {
			recordedStateOfCharge = new TwoColumnTable<Double, Double>();
		}
		initOutputFile();
	}

	public ElectricCostHandler(DESController controler2,
			EnergyConsumptionSamples energyConsumptionSamples2, Events events2,
			String observedVehicleId) {
		this.controler2 = controler2;
		this.energyConsumptionSamples = energyConsumptionSamples2;
		this.events = events2;
		this.observedVehicleId = observedVehicleId;
		if (observedVehicleId != null) {
			recordedStateOfCharge = new TwoColumnTable<Double, Double>();
		}
		initOutputFile();
	}

	private void initEnergyLevel(PersonEvent event) {
		energyLevel.put(event.agentId, new EnergyApplicatonSpecificState(
				fullEnergyLevel));
	}

	public void handleEvent(LinkLeaveEvent event) {
		// for some strange reason, the links, person are not set using the DES
		// controller
		if (controler == null) {
			event.link = controler2.getNetwork().getLink(event.linkId);
			event.setAgent(controler2.getPopulation().getPerson(new IdImpl(event.agentId)));
		}

		// change properties of roads
		// if (event.linkId.equalsIgnoreCase("110") ||
		// event.linkId.equalsIgnoreCase("104")){
		// event.link.setLength(500);
		// event.link.setFreespeed(7.5);
		// }

		if (event.agentId.equalsIgnoreCase(observedVehicleId)) {
			System.out.println();
		}

		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agentId)) {
			initEnergyLevel(event);
		}

		// updated consumed energy for link
		EnergyApplicatonSpecificState state = energyLevel.get(event.agentId);
		// System.out.print(state.energyLevel);
		state.energyLevel -= getEnergyConsumption(event.link);
		// System.out.print(" => " + state.energyLevel);

		// if energy level is below zero: give huge penalty to agent
		if (state.energyLevel <= 0) {
			events.processEvent(new AgentMoneyEvent(event.getTime(), event.getAgent(),
					penaltyForRunningOutOfElectricEnergy));
		}
		recordSOCOfVehicle(event);
	}

	private double getEnergyConsumption(Link link) {
		double freeSpeed = 0;
		if (controler == null) {
			freeSpeed = link.getFreespeed(controler2.getNetwork()
					.getCapacityPeriod());
		} else {
			freeSpeed = link.getFreespeed(controler.getNetwork()
					.getCapacityPeriod());
		}
		double linkLength = link.getLength();
		return energyConsumptionSamples.getInterpolatedEnergyConsumption(
				freeSpeed, linkLength);
	}

	public void reset(int iteration) {
		System.out.println("averageTimeSpentAtWork:" + averageTimeSpentAtWork
				/ energyLevel.size());
		System.out.println("averageTimeSpentAtShop:" + averageTimeSpentAtShop
				/ energyLevel.size());

		// reset variables
		averageTimeSpentAtWork = 0;
		averageTimeSpentAtShop = 0;
		energyLevel.clear();
		
		// only record the specified iteration results
		if (iteration==1){
			relevantIterationReached=true;
		}
	}

	private class EnergyApplicatonSpecificState {

		EnergyApplicatonSpecificState(double energyLevel) {
			this.energyLevel = energyLevel;
		}

		public double energyLevel = 0; // in J
		public double startTimeOfLastAct = 0; // in sec (offset midnight =
		// 0sec)
	}

	public void handleEvent(ActStartEvent event) {
		// for some strange reason, the links, person are not set using the DES
		// controller
		if (controler == null) {
			event.setLink(controler2.getNetwork().getLink(event.getLinkId()));
			event.setAgent(controler2.getPopulation().getPerson(new IdImpl(event.agentId)));
		}

		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agentId)) {
			initEnergyLevel(event);
		}

		// set start time of act
		EnergyApplicatonSpecificState state = energyLevel.get(event.agentId);
		state.startTimeOfLastAct = event.getTime();
		recordSOCOfVehicle(event);
	}

	public void handleEvent(ActEndEvent event) {
		// for some strange reason, the links, person are not set using the DES
		// controller
		if (controler == null) {
			event.setLink(controler2.getNetwork().getLink(event.getLinkId()));
			event.setAgent(controler2.getPopulation().getPerson(new IdImpl(event.agentId)));
		}

		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agentId)) {
			initEnergyLevel(event);
		}

		// update energyLevel (how much the car loaded during the parking) and
		// also put the cost
		// for the charging into the bill (utility function) of the agent

		EnergyApplicatonSpecificState state = energyLevel.get(event.agentId);

		// assumption is, the agent starts immediately charging, until the
		// energyLevel is full
		// TODO: read chargingPower and costPerJule from config file
		double chargingPower = 3500; // in J/s (=Watt)
		// default: 3.5KW => 3500 W
		double costPerJuleAtWork = -1; // in "util"/Euro per Jule
		// 0.09 Euro / kWh = 3600000J => 0.000000025 "euro per jule"
		double costPerJuleAtShop = -1;

		double activityTime = event.getTime() - state.startTimeOfLastAct; // in
		// seconds
		double energyCharged = chargingPower * activityTime; // in J

		// adjust energyCharged (if could charge more than full battery)
		if (state.energyLevel + energyCharged > fullEnergyLevel) {
			energyCharged = fullEnergyLevel - state.energyLevel;
			// System.out.println(energyCharged/chargingPower);
		}

		double costOfEnergy = 0.0; // in "util"/Euro
		if (event.getLinkId().toString().equalsIgnoreCase("100")) {

			// work1
			costOfEnergy = energyCharged * costPerJuleAtWork;
			// System.out.println(activityTime);
			averageTimeSpentAtWork += activityTime;
		} else if (event.getLinkId().toString().equalsIgnoreCase("107")) {
			// work2
			costOfEnergy = energyCharged * costPerJuleAtShop;
			averageTimeSpentAtShop += activityTime;
			// System.out.println("noooo:"+costOfEnergy);
		}

		events.processEvent(new AgentMoneyEvent(event.getTime(), event.getAgent(),
				costOfEnergy));

		state.energyLevel += energyCharged;

		// we need to log the time, when the charging was finished
		double eventTime = state.startTimeOfLastAct + energyCharged
				/ chargingPower;
		recordSOCOfVehicle(event, eventTime);

		// update energy consumption
		if (event.getLinkId().toString().equalsIgnoreCase(selectedLink) && relevantIterationReached) {
			for (int i = (int) Math.round(state.startTimeOfLastAct); i < (int) Math
					.round(state.startTimeOfLastAct + energyCharged
							/ chargingPower); i++) {
				energyConsumptionAtLink[i] += chargingPower;
			}
		}

		// write output event
		// do not write out events, which are caused by the first act end (leaving home)
		if (chargingOutput != null && state.startTimeOfLastAct!=state.startTimeOfLastAct
				+ energyCharged / chargingPower && relevantIterationReached) {
			try {
				chargingOutput.write(event.getLinkId().toString() + "\t" + event.agentId + "\t"
						+ state.startTimeOfLastAct + "\t" + (state.startTimeOfLastAct
						+ (energyCharged) / chargingPower) + "\t" + (state.energyLevel - energyCharged) + "\t" + (state.energyLevel) + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void handleEvent(AgentMoneyEvent event) {
		// System.out.println("money:"+event.amount);
	}

	// SOC = state of charge
	public void printRecordedSOC() {
		System.out.println("observed vehicle:" + observedVehicleId);
		System.out.println("time\tSOC [J]");

		for (int i = 0; i < recordedStateOfCharge.size(); i++) {
			System.out.println(recordedStateOfCharge.getC1(i) + "\t"
					+ recordedStateOfCharge.getC2(i));
		}
	}

	private void recordSOCOfVehicle(PersonEvent event) {
		recordSOCOfVehicle(event, event.getTime());
	}

	private void recordSOCOfVehicle(PersonEvent event, double time) {
		if (event.agentId.toString().equalsIgnoreCase(observedVehicleId) && relevantIterationReached) {
			recordedStateOfCharge.add(time,
					energyLevel.get(observedVehicleId).energyLevel);
		}
	}

	public void handleEvent(LinkEnterEvent event) {
		// initialize the energyLevel at the beginning to full energy
		if (!energyLevel.containsKey(event.agentId)) {
			initEnergyLevel(event);
		}
		recordSOCOfVehicle(event);

		// printSomeSOCStatus(event.time);
	}

	// SOC: State of Charge
	// prints only a few unique energies
	public void printSomeSOCStatus(double time) {

		// key: energyValue
		// value: agentId
		HashMap<Double, String> tmpEnergy = new HashMap<Double, String>();

		for (String agentId : energyLevel.keySet()) {
			tmpEnergy.put(energyLevel.get(agentId).energyLevel, agentId);
			// System.out.println(agentId+"\t"+energyLevel.get(agentId).energyLevel);
		}

		// only print, if more than 5 different SOCs
		if (tmpEnergy.size() < 5) {
			return;
		}

		System.out.println("observation time:" + time);
		System.out.println("agentId\tSOC [J]");

		for (Double energyLevel : tmpEnergy.keySet()) {
			System.out.println(tmpEnergy.get(energyLevel) + "\t" + energyLevel);
		}
		System.exit(0);
	}

	public void printEnergyConsumptionAtSelectedLink() {
		System.out.println("selected link:" + selectedLink);
		System.out.println("timeId\tSOC [J]");
		for (int i = 0; i < energyConsumptionAtLink.length; i++) {
			if (energyConsumptionAtLink[i] != 0.0) {
				System.out.println(i + "\t" + energyConsumptionAtLink[i]);
			}
		}

	}
	
	public void tidyup(){
		if (chargingOutput==null){
			return;
		}
		try {
			chargingOutput.flush();
			chargingOutput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
