package fr.irtx.lead.matsim.congestion;

import java.util.List;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class CongestionCalculator {
	private final Network network;
	private final LeastCostPathCalculator router;

	public CongestionCalculator(LeastCostPathCalculator router, Network network) {
		this.router = router;
		this.network = network;
	}

	public void calculateUncongestedTravelTimes(List<CongestionRecord> records) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Congestion calculation ...", records.size());
		progress.start();

		for (CongestionRecord record : records) {
			Link originLink = network.getLinks().get(record.originId);
			Link destinationLink = network.getLinks().get(record.destinationId);

			Node originNode = originLink.getToNode();
			Node destinationNode = destinationLink.getFromNode();

			Path path = router.calcLeastCostPath(originNode, destinationNode, record.departureTime, null, null);
			record.uncongestedTravelTime = path.travelTime;
			record.travelTime = record.arrivalTime - record.departureTime;

			progress.update();
		}

		progress.close();
	}
}
