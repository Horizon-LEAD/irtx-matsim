package fr.irtx.lead.matsim.freight;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Verify;

import fr.irtx.lead.matsim.freight.FreightStop.FreightStopLocation;
import fr.irtx.lead.matsim.freight.FreightStop.FreightStopType;

public class FreightConfigurator {
	private final File freightPath;
	private final Set<String> modes = new HashSet<>();

	public FreightConfigurator(File freightPath) {
		this.freightPath = freightPath;
	}

	public Set<String> getModes() {
		return modes;
	}

	public void configure(Config config, Scenario scenario)
			throws JsonParseException, JsonMappingException, IOException {
		FreightData data = new ObjectMapper().readValue(freightPath, FreightData.class);

		ActivityParams startActivityParams = new ActivityParams("freight:start");
		startActivityParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(startActivityParams);

		ActivityParams pickupActivityParams = new ActivityParams("freight:pickup");
		pickupActivityParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(pickupActivityParams);

		ActivityParams dropoffActivityParams = new ActivityParams("freight:delivery");
		dropoffActivityParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(dropoffActivityParams);

		ActivityParams endActivityParams = new ActivityParams("freight:end");
		endActivityParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(endActivityParams);

		for (FreightVehicleType vehicleTypeData : data.vehicleTypes) {
			String mode = "freight:" + vehicleTypeData.id;
			Verify.verify(modes.add(mode));

			Vehicles vehicles = scenario.getVehicles();
			VehiclesFactory factory = vehicles.getFactory();

			VehicleType vehicleType = factory.createVehicleType(Id.create(mode, VehicleType.class));
			vehicleType.setMaximumVelocity(vehicleTypeData.speed_km_h * 3.6);

			vehicles.addVehicleType(vehicleType);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setEstimator(mode, EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);
			eqasimConfig.setCostModel(mode, EqasimModeChoiceModule.ZERO_COST_MODEL_NAME);
		}

		Set<String> networkModes = new HashSet<>();
		networkModes.addAll(config.plansCalcRoute().getNetworkModes());
		networkModes.addAll(modes);
		config.plansCalcRoute().setNetworkModes(networkModes);

		for (String mode : modes) {
			ModeParams modeParams = new ModeParams(mode);
			config.planCalcScore().addModeParams(modeParams);

			for (Link link : scenario.getNetwork().getLinks().values()) {
				Set<String> linkModes = new HashSet<>();
				linkModes.addAll(link.getAllowedModes());
				linkModes.add(mode);
				link.setAllowedModes(linkModes);
			}
		}

		PopulationFactory factory = scenario.getPopulation().getFactory();
		int personIndex = 0;

		for (FreightVehicle vehicleData : data.vehicles) {
			String mode = "freight:" + vehicleData.vehicleType;
			Coord currentLocation = null;

			Person person = factory.createPerson(Id.createPersonId("freight:" + personIndex++));
			scenario.getPopulation().addPerson(person);

			Plan plan = factory.createPlan();
			person.addPlan(plan);

			for (FreightStop stop : vehicleData.stops) {
				boolean isFirst = currentLocation == null;

				if (!isFirst && CoordUtils.calcEuclideanDistance(currentLocation, convert(stop.location)) > 100.0) {
					Leg leg = factory.createLeg(mode);
					plan.addLeg(leg);
				}

				Activity activity = factory.createActivityFromCoord("freight:" + stop.type.toString(),
						convert(stop.location));

				if (stop.type.equals(FreightStopType.start)) {
					activity.setEndTime(stop.departureTime);
				} else if (!stop.type.equals(FreightStopType.end)) {
					activity.setStartTime(stop.arrivalTime);
					activity.setMaximumDuration(stop.departureTime - stop.arrivalTime);
				} else if (!stop.type.equals(FreightStopType.end)) {
					activity.setStartTime(stop.arrivalTime);
				}

				plan.addActivity(activity);

				currentLocation = convert(stop.location);
			}
		}

	}

	private Coord convert(FreightStopLocation location) {
		return new Coord(location.x, location.y);
	}
}
