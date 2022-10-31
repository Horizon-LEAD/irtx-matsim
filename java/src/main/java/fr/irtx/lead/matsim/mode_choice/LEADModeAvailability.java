package fr.irtx.lead.matsim.mode_choice;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class LEADModeAvailability implements ModeAvailability {
	private final Set<String> freightModes;
	private final IDFModeAvailability delegate;

	public LEADModeAvailability(Set<String> freightModes, IDFModeAvailability delegate) {
		this.freightModes = freightModes;
		this.delegate = delegate;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		for (DiscreteModeChoiceTrip trip : trips) {
			if (freightModes.contains(trip.getInitialMode())) {
				return Collections.singleton(trip.getInitialMode());
			}
		}

		return delegate.getAvailableModes(person, trips);
	}
}
