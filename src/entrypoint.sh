#!/bin/bash

#Set fonts
NORM=`tput sgr0`
BOLD=`tput bold`
REV=`tput smso`

function show_usage () {
    echo -e "${BOLD}Basic usage:${NORM} entrypoint.sh [-vh] FIN_PERSONS FIN_HOMES SEED SCALING OUT_PATH"
}

function show_help () {
    echo -e "${BOLD}eentrypoint.sh${NORM}: Runs the Parcels Model"\\n
    show_usage
    echo -e "\n${BOLD}Required arguments:${NORM}"
    echo -e "${REV}SYNPOP_CONFIG_XML${NORM}\t the config xml from synpop"
    echo -e "${REV}FIN_HOMES${NORM}\t the homes gpkg input file"
    echo -e "${REV}SEED${NORM}\t the random seed"
    echo -e "${REV}SCALING${NORM}\t the scaling factor"
    echo -e "${REV}OUT_PATH${NORM}\t the output path"\\n
    echo -e "${BOLD}Optional arguments:${NORM}"
    echo -e "${REV}-v${NORM}\tSets verbosity level"
    echo -e "${REV}-h${NORM}\tShows this message"
    echo -e "${BOLD}Examples:${NORM}"
    echo -e "entrypoint.sh -v persons.xlsx homes.gpkg 1234 0.1 ./output/"
}

##############################################################################
# GETOPTS                                                                    #
##############################################################################
# A POSIX variable
# Reset in case getopts has been used previously in the shell.
OPTIND=1

# Initialize vars:
verbose=0

# while getopts
while getopts 'hv' OPTION; do
    case "$OPTION" in
        h)
            show_help
            kill -INT $$
            ;;
        v)
            verbose=1
            ;;
        ?)
            show_usage >&2
            kill -INT $$
            ;;
    esac
done

shift "$(($OPTIND -1))"

leftovers=(${@})
SYNPOP_CONFIG_XML=${leftovers[0]}
TRACES=${leftovers[1]}
OUT_PATH=${leftovers[2]}

##############################################################################
# Input checks                                                               #
##############################################################################
if [ ! -d "${OUT_PATH}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} output directory\n"; show_usage; exit 1
fi

##############################################################################
# Execution                                                                  #
##############################################################################
java -Xmx20g -cp /srv/app/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
	--config-path ${SYNPOP_CONFIG_XML} \
	--output-path ${OUT_PATH}

echo "1: output"
ls ${OUT_PATH}

java -Xmx20G -cp /srv/app/java/target/lead-matsim-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter \
	--config-path ${SYNPOP_CONFIG_XML} \
	--extent-path /srv/app/data/perimeter_lyon.shp \
	--output-path ${OUT_PATH} \
	--prefix perimeter_ \
	--config:plans.inputPlansFile ${OUT_PATH}/output_plans.xml.gz

echo "2: output"
ls ${OUT_PATH}

java -Xmx20g -cp /srv/app/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
    --config-path ${OUT_PATH}/perimeter_config.xml \
    --output-path ${OUT_PATH} \
    --freight-path ${TRACES}

echo "3: output"
ls ${OUT_PATH}

# papermill /srv/app/src/congestion-analysis.ipynb /dev/null \
#     -psimulation_output_path /home/ubuntu/irtx-matsim/output/output_${scenario} \
#     -pkpi_path /home/ubuntu/irtx-matsim/output/congestion_${scenario}.json


