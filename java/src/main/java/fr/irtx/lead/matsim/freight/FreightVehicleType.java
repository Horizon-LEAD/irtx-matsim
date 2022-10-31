package fr.irtx.lead.matsim.freight;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreightVehicleType {
	@JsonProperty("id")
	public String id;

	@JsonProperty("speed_km_h")
	public double speed_km_h;
}
