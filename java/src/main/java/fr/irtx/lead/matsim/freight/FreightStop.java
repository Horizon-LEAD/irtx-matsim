package fr.irtx.lead.matsim.freight;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreightStop {
	@JsonProperty("location")
	public FreightStopLocation location = new FreightStopLocation();

	@JsonProperty("arrival_time")
	public double departureTime = 0.0;

	@JsonProperty("departure_time")
	public double arrivalTime = 0.0;

	@JsonProperty("type")
	public FreightStopType type;

	public static enum FreightStopType {
		start, end, pickup, delivery
	}

	public static class FreightStopLocation {
		@JsonProperty("x")
		public double x = 0.0;

		@JsonProperty("y")
		public double y = 0.0;
	}
}
