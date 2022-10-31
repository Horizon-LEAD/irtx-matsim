package fr.irtx.lead.matsim.mode_choice;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import fr.irtx.lead.matsim.freight.FreightConfigurator;

public class LEADModeChoiceModule extends AbstractEqasimExtension {
	private final FreightConfigurator freightConfigurator;

	public LEADModeChoiceModule(FreightConfigurator freightConfigurator) {
		this.freightConfigurator = freightConfigurator;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability("LEAD").to(LEADModeAvailability.class);
	}

	@Provides
	@Singleton
	public LEADModeAvailability provideLEADModeAvailability() {
		return new LEADModeAvailability(freightConfigurator.getModes(), new IDFModeAvailability());
	}
}
