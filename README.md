# IRTX MATSim Implementation

## TODO

- Test that this works with the prepared input and output data
- JSON file

## Introduction

This model is a wrapper around the multi-agent transport simulation framework MATSim:

> https://matsim.org/

MATSim is a framework used by more than 50 research institutes world-wide to set
up a wide variety of transport simulations that are able to model the dynamic interaction
between travelers and mobility service providers. There are extensions to simulate
car-sharing services, on-demand mobility, micromobility and standardized analysis
tools for emissions, noise and externality-based traffic control.

A MATSim simulation consists of multiple iterations. In each iteration, a mobility
simulation with all agents is performed, leading to phenomena such as congestion.
In a second step, agents make decisions based on the observed traffic characteristics,
for instance, they might choose a different mode of transport for certain trips in
their daily scheduled. This way, agents adapt to the traffic conditions. By running
this loop between mobility simulation and decision-making, the decisions and daily
mobility pattern go into equilibrium with the traffic conditions. The final iteration
can be observed and indicators and visualisations can be derived from it.

For the LEAD project, only a part of the overall functionality has been packaged
and customized to be used mainly in the Lyon living lab. However, the model remains
sufficiently generic and allows to simulate a city (given that the relevant input
data is provided) with the mobility traces of all its inhabitants. Additionally,
the LEAD repository adds the functionality to add commercial vehicles that have
been generated in an upstream model such as JSprit.

Specifically, the model for Lyon is based on the synthetic population data
generated by the synthetic population model and the vehicles traces generated
by JSprit. In order to generate those inputs, see the documentation of the
upstream models.

The MATSim model prepares the output for downstream emissions and noise analysis.
Furthermore, indicators on congestion can be obtained.

## Requirements

### Software requirements

To run the model, the environment needs to be prepared:

- A `conda` or `mamba` environment needs to be set up in which the Python code of the model is run. The LEAD repository provides `environment.yml` which describes the `conda` environment and all dependencies.

- Note that some of the dependencies installed via `conda > pip` need a recent compiler available on the system. Additionally, the MATSim needs to have access to the fonts available on the system. On an Ubunutu system, it suffices to `apt install build-essential fontconfig`.

- A `Java` runtime needs to be present on the executing machine. It is recommended to set up an **Adoptium OpenJDK 11** (https://adoptium.net).

- A recent version of `maven` needs to be installed, version `3.6.3` has been tested: https://maven.apache.org/

It is recommended to set up the environment on a Linux machine, the following
executables should then be callable from the command line: `java`, `mvn`.

### Input / Output

The model can be used in two different setups:

- Running a baseline simulation for a larger region (Rhône-Alpes around Lyon in
  the case of the Lyon living lab). Such a simulation is run to establish a baseline
  congestion scenario where all agents adapt their itineraries according to the
  endogoenous travel times.

- Running a cut simulation for a focus area (Confluence inside Lyon in the case
  of the Lyon living lab). To do so, the converged regional simulation can be cut
  such that subsequent policy scenarios only need to be simulated on the smaller
  study area. In such a simulation, modifications can be added to simulate policy
  cases, e.g., including additional commercial vehicle traces as in the present
  case.

#### Input

**Baseline simulation**

In order to perform the regional baseline simulation, MATSim-compatible input
data needs to be provided. This input data contains the following files (usually
with an added prefix to distinguish between scenarios). They are provided as plain
or compressed XML files:

- `config.xml`: Provides configuration information to the simulation and references all the other files mentioned below.
- `network.xml.gz`: Describes the network topology
- `households.xml.gz`: Describes all households that are present in the simulation including sociodemographic attributes
- `population.xml.gz`: Contains information on all the individual persons that are simulated including their daily mobility patterns with activities and trips in between
- `facilities.xml.gz`: Localizes addresses and other private and public facilities that are referenced in the mobility chains
- `transit_schedule.xml.gz`: Describes the public transport offer in the simulation area
- `transit_vehicles.xml.gz`: Describes the public transport vehicles that are offering the defined transit services

These input files can be generated, for instance, using the population synthesis
model provided as a downstream stage in the LEAD modeling library. A detailed
description of their contains is out of scope at this place as they are rather
complex. Detailed information can be found in the MATSim Book:

> Horni, A., Nagel, K., Axhausen, K.W. (Eds.), 2016. The Multi-Agent Transport Simulation MATSim. Ubiquity Press. https://doi.org/10.5334/baw

Note that the regional simulation usually only needs to be run once and all detailed
analyses are performed on a smaller cut-out. Further below it is described how such
a cut out can be created using the wrapped model. In case such a cut-out is to be created,
a geographic perimeter needs to be defined using an input file in Shapefile format. For the
Lyon use case, as respective file is provided in `scenario_data/perimeter.shp`.

**Commercial vehicle simulation**

To run the local simulations and include the commercial vehicles from the LEAD
pipeline, output data from the JSprit - MATSim connector needs to be provided
in JSON format, e.g. `input_traces.json`. This input file is structured as
follows:

```json
{
  "vehicle_types": [
    { "id": "van", "speed_km_h": 40.0 }
  ],
  "vehicles": [
    {
      "vehicle_type": "van",
      "stops": []
    }
  ]
}
```

First, a list of vehicle types is given, after a list of individual vehicles
with their stops. Each vehicle refers to a vehicle type which is defined by
its maximum road speed. Futhermore, each vehicle has a list of stops, which
are defined as follows:

```json
{
  "type": "start",
  "arrival_time": 28800.0,
  "departure_time": 28800.0,
  "location": {
    "x": 841484.6518412721,
    "y": 6517523.922895046
  }
}
```

Each stop has a type that can either be `start` (for the initial stop at the depot),
`end` (for the final stop at the depot), `pickup` (when picking up an item),
`delivery` (when delivering an item). For each stop, an arrival time and departure
time is given, which is defined by the upstream JSprit and connector models. Finally,
the location of the stop is defined using coordinates `x` and `y`. Not that these are
not longitude and latitude as in the upstream models, but projected coordinates
using a coordinate reference system that needs to be the same as the one in which
all other model files (`population.xml.gz`, `network.xml.gz`) are described. For the
specific use case of Lyon the French standard projection `EPSG:2154` is used.

#### Output

The output of a MATSim simulation is a directory with various information. The
most important output is a file called `events.xml.gz` which contains all the
individual events that happened in the simulation (agent enters/leaves link,
agent starts/ends a trip/activity, ...). It is the major output file from which
second-order analyses can be derived. The relevant files for the use of the
MATSim model in LEAD are:

- `/output/trips.csv`: A file containing all trips that happened during the simulation including their mode of transport and the covered distance. Note that each commercial vehicle type defined in the input files is represented as one individual mode identified as `freight:{vehicle_type_id}`. The trips file is also used to generate the input for the downstream noise and emission models using the respective connectors.
- `/output/congestion.csv`: A file specifically develope in the MATSim implementation for LEAD which compares all `car` trips in terms of their recorded travel time in the simulation and the direct freeflow travel time of the same trip. The LEAD repository contains a script that allows to generate high-level congestion KPIs based on this file (see below).
- `/output/output_plans.xml.gz`: Contains the final daily mobility plans of all agents. This file is used as a basis for cutting a smaller scope simulation from a larger one.

To generate the aggregated congestion information, the notebook `Congestion Analysis.ipynb` can be used (see below). The output of this notebook is a `json` file that is structued as follows:

```json
{
  "total_delay_min": 114752.59,
  "delay_per_driver_min": 14.66
}
```

The first slot, `total_delay_min` gives the total delay that has been accumulated by
the population compared to a freeflow travel time for their car trips in minutes.
The second slot `delay_per_driver_min` divides this value by the number of people that
have used a car during the day.

# Building the model

The MATSim model is provided as Java code. To run it, it first needs to be built using
the Maven build system. For that purpose, one needs to enter the `java` directory
of the LEAD repository and call `mvn package`:

```bash
cd /irtx-matsim/java
mvn package
```

The build process should download all necessary Maven dependencies including
the MATSim and finish without errors. After, the built model should be
present in

```
/irtx-matsim/java/target/lead-matsim-1.0.0.jar
```

The `jar` file can be saved in a fixed location. As long as the model is not
changed, it can be reused for multiple model runs. To test whether the `jar` has
been build successfully, call

```bash
java -cp /path/to/lead-jsprit-1.0.0.jar fr.irtx.lead.matsim.RunVerification
```

which should respond by the message `It works!`.

# Running the model

The model repository provides the code to run the MATSim model itself and to
generate congestion KPIs. Note that in the proposed LEAD use case for Lyon,
the MATSim model is called twice (see above): Once to create a converged simulation
state for the Rhône-Alpes region around Lyon, and once (or multiple times) to run
a much smaller cut-out of the Confluence half-island with varying simulation
parameters.

The model, hence, provides three functionalities:
- Run a MATSim simulation
- Run a simulation perimeter
- Run congestion analysis

These are described in detail in the following.

**MATSim simulation**

To run a MATSim simulation, the respective jar needs to be built first. We
assume that the configuration file of the underlying MATSim scenario is located
at `/path/to/config.xml`. The simulation can then be started in the following
way:

```bash
java -Xmx20g -cp java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /path/to/config.xml \
  --output-path /path/to/output_directory \
  --threads 8 \
  --iterations 120 \
  --freight-path /path/to/traces.json
```

The first line is mandatory with the path to the built `jar` file that needs
to be adapted. The following lines represent parameters. The **mandatory**
parameters are detailed in the following table:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--config-path`          | String                            | Path to the configuration file
`--output-path`         | String                            | Path to where the result will be saved

The following **optional** parameter is available:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--freight-path`          | String              | Path to the vehicle traces of the additional commercial vehicles

Finally, **technical** parameters exist that can be configured:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--random-seed`       | Integer (default `1234`)          | Allows to perform ensemble runs by providing different initialization seeds of the optimization
`--iterations`        | Integer (default `120`)         | Sets the number of iterations per operator with higher accuracy with increasing values (but also higher runtime)
`--threads`           | Integer (default `12`)             | Allows to set the number of threads that are used for the optimization

Note that the memory available to Java can be configured using the `-Xmx` option and by appending a size of the format `1024M` to define the amount in megabytes or `20G` to define the amount in gigabytes.

**Cutting a simulation**

Using another run class in from the same `jar` file, one can cut a generated simulation as follows:

```bash
java -Xmx20G -cp java/target/lead-matsim-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter \
  --config-path /path/to/config.xml \
  --extent-path /path/to/perimeter.shp \
  --output-path /path/to/output_directory \
  --prefix perimeter_2022_5pct_ \
  --config:plans.inputPlansFile /path/to/output_plans.xml.gz \
  --threads 12
```

The **mandatory** parameters are detailed in the following table:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--config-path`         | String                            | Path to the configuration file of the original simulation
`--extent-path`         | String                            | Path to the geographic file describing the extent of the cut-out (must be in the same CRS as the simulation data)
`--output-path`         | String                            | Path to a directory into which the cut simulation data will be saved. *Must exist.*

The following **optional** parameter is available:

Parameter             | Values                            | Description
---                   | ---                               | ---
`--prefix`          | String              | All generated files (network, population, ...) will be prepended by this prefix
`--threads`          | Integer (default `12`)              | Number of threads to be used for cuting
`--config:plans.inputPlansFile`          | String              | Path to the `output_plans.xml.gz` file of the output of the converged simulation that should be used as a basis for cutting. *Attention: It is recommended to provide an absolute path here, as any relative path is interpreted as relative to the configuration file*.

After running the cutter, you'll find the exact same input files for a MATSim simulation as described above in the `output-path`, possibly with a custom prefix according to the command line parameters. This simulation can then be restarted as a standard MATSim simulation (see above).

**Congestion analysis**

Finally, the repository contains `Congestion.ipynb`, a notebook which allows to
generate aggregated congestion indicators from the MATSim simulation. To run it,
call it through the `papermill` command line utility (which is installed as a
dependency in the `conda` environment) as described below:

```bash
papermill "Congestion Analysis.ipynb" /dev/null \
  -psimulation_output_path /path/to/irtx-matsim/output \
  -pkpi_path /path/to/congestion_kpi.json \
  -pcutoff_min 60.0
```

The **mandatory** parameters are as follows:

Parameter             | Values                            | Description
---                   | ---                               | ---
`simulation_output_path`         | String                            | Path to the output directory of a MATSim simulation
`kpi_path`         | String                            | Path where to save the aggregated congestion information

There is one **optional** parameter:

`cutoff_min`         | Real (default `60`)                            | To avoid outliers, it defines a cutoff value for the observed delays

## Standard scenarios

For the Lyon living lab, some standard simulations can be run. They are based
on the output of the synthetic population model (Population 2022, Population 2030)
and the output of the JSprit scenarios (Baseline 2022, UCC 2022, UCC 2030).

**Baseline**

First, the baseline simulations can be run, based on the synthetic population
output. The command to do so is:

```bash
mkdir -p /path/to/irtx-matsim/output/output_lead_{year}

java -Xmx20g -cp java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /path/to/irtx-synpop/output/lead_{year}_5pct_config.xml \
  --output-path /path/to/irtx-matsim/output/output_lead_{year}
```

Here, `year` can be replaced by `2022` or `2030`.

**Cutting**

Both cases can be cut to the Lyon Confluence area. For that, the perimeter is
provided in the repository as `scenario_data/perimeter.shp`. The command is
as follows:

```bash
java -Xmx20G -cp java/target/lead-matsim-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter \
  --config-path /path/to/irtx-synpop/output/lead_{year}_5pct_config.xml \
  --extent-path scenario_data/perimeter.shp \
  --output-path /path/to/irtx-matsim/output \
  --prefix perimeter_{year}_ \
  --config:plans.inputPlansFile /path/to/irtx-matsim/output/output_lead_{year}/output_plans.xml.gz
```

Again, `year` can be replaced by `2022` or `2030`. Note that the baseline simulation and cutting only needs to be redone when the upstream synthetic population is changed.

**Scenario simulation**

Based on the cut perimeters, the local simulations for Confluence with the three
scenarios from the JSprit model can be run. First for *Baseline 2022*:

```bash
mkdir -p /path/to/irtx-matsim/output/output_baseline_{year}

java -Xmx20g -cp java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /path/to/irtx-matsim/output/perimeter_2022_config.xml \
  --output-path /path/to/irtx-matsim/output/output_baseline_{year} \
  --freight-path /path/to/irtx-jsprit-matsim-connector/output/traces_baseline_2022.json
```

Then for *UCC 2022* and *UCC 2030* with `{year}` replaces by the respective year:

```bash
mkdir -p /path/to/irtx-matsim/output/output_baseline_{year}

java -Xmx20g -cp java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /path/to/irtx-matsim/output/perimeter_{year}_config.xml \
  --output-path /path/to/irtx-matsim/output/output_ucc_{year} \
  --freight-path /path/to/irtx-jsprit-matsim-connector/output/traces_ucc_{year}.json
```

**Congestion**

For each scenario, the congestion KPIs can be calculated:

```bash
papermill "Congestion Analysis.ipynb" /dev/null \
  -psimulation_output_path /path/to/irtx-matsim/output/output_baseline_2022/congestion.csv \
  -pkpi_path /path/to/irtx-matsim/output/congestion_baseline_2022.json
```

```bash
papermill "Congestion Analysis.ipynb" /dev/null \
  -psimulation_output_path /path/to/irtx-matsim/output/output_ucc_2022/congestion.csv \
  -pkpi_path /path/to/irtx-matsim/output/congestion_ucc_2022.json
```

```bash
papermill "Congestion Analysis.ipynb" /dev/null \
  -psimulation_output_path /path/to/irtx-matsim/output/output_ucc_2030/congestion.csv \
  -pkpi_path /path/to/irtx-matsim/output/congestion_ucc_2030.json
```
