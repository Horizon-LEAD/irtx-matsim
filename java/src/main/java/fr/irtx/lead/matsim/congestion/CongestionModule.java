package fr.irtx.lead.matsim.congestion;

import java.util.Collections;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CongestionModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(CongestionListener.class);
	}

	@Provides
	@Singleton
	public CongestionListener provideCongestionListener(OutputDirectoryHierarchy outputHierarchy,
			EventsManager eventsManager, CongestionCalculator calculator) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(getConfig());
		int analysisInterval = eqasimConfig.getTripAnalysisInterval();

		return new CongestionListener(analysisInterval, outputHierarchy, eventsManager, calculator);
	}

	@Provides
	@Singleton
	public CongestionCalculator provideCongestionCalculator(Network network) {
		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));

		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(carNetwork,
				new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

		return new CongestionCalculator(router, carNetwork);
	}
}
