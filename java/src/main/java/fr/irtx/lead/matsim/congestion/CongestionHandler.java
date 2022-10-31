package fr.irtx.lead.matsim.congestion;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

public class CongestionHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	private final Set<String> modes = new HashSet<>();

	private final IdMap<Person, CongestionRecord> ongoing = new IdMap<>(Person.class);
	private final List<CongestionRecord> records = new LinkedList<>();

	public CongestionHandler() {
		this.modes.add("car");
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (modes.contains(event.getLegMode())) {
			ongoing.put(event.getPersonId(), new CongestionRecord(event.getLinkId(), event.getTime()));
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (modes.contains(event.getLegMode())) {
			CongestionRecord trip = ongoing.remove(event.getPersonId());
			trip.arrivalTime = event.getTime();
			trip.destinationId = event.getLinkId();
			records.add(trip);
		}
	}

	@Override
	public void reset(int iteration) {
		this.records.clear();
		this.ongoing.clear();
	}

	public List<CongestionRecord> getRecords() {
		return records;
	}
}
