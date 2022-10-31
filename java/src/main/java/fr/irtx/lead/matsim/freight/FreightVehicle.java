package fr.irtx.lead.matsim.freight;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreightVehicle {
	@JsonProperty("stops")
	public List<FreightStop> stops = new LinkedList<>();

	@JsonProperty("vehicle_type")
	public String vehicleType;
}
