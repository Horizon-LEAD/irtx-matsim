package fr.irtx.lead.matsim.congestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CongestionRecord {
	public Id<Link> originId;
	public Id<Link> destinationId;

	public double departureTime;
	public double arrivalTime;

	public double travelTime;
	public double uncongestedTravelTime;

	CongestionRecord(Id<Link> originId, double departureTime) {
		this.departureTime = departureTime;
		this.originId = originId;
	}
}
