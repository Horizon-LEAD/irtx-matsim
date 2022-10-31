package fr.irtx.lead.matsim.freight;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreightData {
	@JsonProperty("vehicles")
	public List<FreightVehicle> vehicles = new LinkedList<>();

	@JsonProperty("vehicle_types")
	public List<FreightVehicleType> vehicleTypes = new LinkedList<>();
}
