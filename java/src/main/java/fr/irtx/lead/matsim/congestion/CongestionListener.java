package fr.irtx.lead.matsim.congestion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.io.Files;

public class CongestionListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
	private final OutputDirectoryHierarchy outputHierarchy;
	private final EventsManager eventsManager;
	private final CongestionCalculator calculator;

	private final int analysisInterval;
	private CongestionHandler handler;

	public CongestionListener(int analysisInterval, OutputDirectoryHierarchy outputHierarchy,
			EventsManager eventsManager, CongestionCalculator calculator) {
		this.analysisInterval = analysisInterval;
		this.outputHierarchy = outputHierarchy;
		this.eventsManager = eventsManager;
		this.calculator = calculator;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (analysisInterval > 0 && (event.getIteration() % analysisInterval == 0 || event.isLastIteration())) {
			handler = new CongestionHandler();
			eventsManager.addHandler(handler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (handler != null) {
			try {
				BufferedWriter writer = IOUtils.getBufferedWriter(
						outputHierarchy.getIterationFilename(event.getIteration(), "congestion.csv"));

				calculator.calculateUncongestedTravelTimes(handler.getRecords());

				writer.write(String.join(";",
						Arrays.asList("departure_time", "arrival_time", "simulatedTravelTime", "uncongestedTravelTime"))
						+ "\n");

				for (CongestionRecord record : handler.getRecords()) {
					writer.write(String.join(";", Arrays.asList( //
							String.valueOf(record.departureTime), //
							String.valueOf(record.arrivalTime), //
							String.valueOf(record.travelTime), //
							String.valueOf(record.uncongestedTravelTime) //
					)) + "\n");
				}

				writer.close();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}

			eventsManager.removeHandler(handler);
			handler = null;
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), "congestion.csv"));
			File outputFile = new File(outputHierarchy.getOutputFilename("congestion.csv"));
			Files.copy(iterationFile, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
