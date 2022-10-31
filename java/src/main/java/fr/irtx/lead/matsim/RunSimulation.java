package fr.irtx.lead.matsim;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import fr.irtx.lead.matsim.congestion.CongestionModule;
import fr.irtx.lead.matsim.freight.FreightConfigurator;
import fr.irtx.lead.matsim.mode_choice.LEADModeChoiceModule;

public class RunSimulation {
	static public void main(String[] args)
			throws ConfigurationException, JsonParseException, JsonMappingException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.allowPrefixes("freight-path") //
				.build();
		
		// output-path
		// threads
		// iterations
		// random seed
		

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		DiscreteModeChoiceConfigGroup.getOrCreate(config).setModeAvailability("LEAD");

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		if (cmd.hasOption("freight-path")) {
			File freightPath = new File(cmd.getOptionStrict("freight-path"));
			FreightConfigurator configurator = new FreightConfigurator(freightPath);
			configurator.configure(config, scenario);
			controller.addOverridingModule(new LEADModeChoiceModule(configurator));
		}

		controller.addOverridingModule(new CongestionModule());

		controller.run();
	}
}
