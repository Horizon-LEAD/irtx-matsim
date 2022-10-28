java -Xmx6g -cp java/target/lead-matsim-1.0.0.jar org.eqasim.ile_de_france.RunSimulation \
  --config-path /home/shoerl/lead/packaging/static_data/irtx-synpop/output/lead_2022_5pct_config.xml \
  --config:controler.outputDirectory data/output_2022 \
  --config:global.numberOfThreads 8 \
  --config:qsim.numberOfThreads 8 \
  --config:controler.lastIteration 20

#java -Xmx6g -cp java/target/lead-matsim-1.0.0.jar org.eqasim.ile_de_france.RunSimulation \
#  --config-path /home/shoerl/lead/packaging/static_data/irtx-synpop/output/lead_2030_5pct_config.xml \
#  --config:controler.outputDirectory data/output_2030 \
#  --config:global.numberOfThreads 8 \
#  --config:qsim.numberOfThreads 8 \
#  --config:controler.lastIteration 20
