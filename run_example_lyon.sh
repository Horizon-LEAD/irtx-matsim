set -e

## Prepare
cd /home/ubuntu/irtx-matsim
mkdir /home/ubuntu/irtx-matsim/output

## Create environment
conda env create -f environment.yml -n matsim

## Activate environment
conda activate matsim

## Build model

cd /home/ubuntu/irtx-matsim/java
mvn package
cd /home/ubuntu/irtx-matsim

## Run baseline simulations
for year in 2022 2030; do
	java -Xmx20g -cp /home/ubuntu/irtx-matsim/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
	  --config-path /home/ubuntu/irtx-synpop/output/lead_${year}_5pct_config.xml \
	  --output-path /home/ubuntu/irtx-matsim/output/output_lead_${year}
done

## Cutting perimeter
for year in 2022 2030; do
	java -Xmx20G -cp /home/ubuntu/irtx-matsim/java/target/lead-matsim-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter \
	  --config-path /home/ubuntu/irtx-synpop/output/lead_${year}_5pct_config.xml \
	  --extent-path /home/ubuntu/irtx-matsim/data/perimeter_lyon.shp \
	  --output-path /home/ubuntu/irtx-matsim/output \
	  --prefix perimeter_${year}_ \
	  --config:plans.inputPlansFile /home/ubuntu/irtx-matsim/output/output_lead_${year}/output_plans.xml.gz
done

## Run scenarios
java -Xmx20g -cp /home/ubuntu/irtx-matsim/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /home/ubuntu/irtx-matsim/output/perimeter_2022_config.xml \
  --output-path /home/ubuntu/irtx-matsim/output/output_baseline_2022 \
  --freight-path /home/ubuntu/irtx-jsprit-matsim-connector/output/traces_baseline_2022.json

java -Xmx20g -cp /home/ubuntu/irtx-matsim/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /home/ubuntu/irtx-matsim/output/perimeter_2022_config.xml \
  --output-path /home/ubuntu/irtx-matsim/output/output_ucc_2022 \
  --freight-path /home/ubuntu/irtx-jsprit-matsim-connector/output/traces_ucc_2022.json

java -Xmx20g -cp /home/ubuntu/irtx-matsim/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
  --config-path /home/ubuntu/irtx-matsim/output/perimeter_2022_config.xml \
  --output-path /home/ubuntu/irtx-matsim/output/output_ucc_2030 \
  --freight-path /home/ubuntu/irtx-jsprit-matsim-connector/output/traces_ucc_2030.json

## Congestion
for scenario in baseline_2022 ucc_2022 ucc_2030; do
	papermill "Congestion Analysis.ipynb" /dev/null \
	  -psimulation_output_path /home/irtx-matsim/matsim/output/output_${scenario} \
    -pkpi_path /home/ubuntu/irtx-matsim/output/congestion_${scenario}.json
done
