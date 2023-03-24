#!/bin/bash

#Set fonts
NORM=`tput sgr0`
BOLD=`tput bold`
REV=`tput smso`

function show_usage () {
    echo -e "${BOLD}Basic usage:${NORM} entrypoint.sh [-vh] SYNPOP_CONFIG_XML TRACES OUT_PATH"
}

function show_help () {
    echo -e "${BOLD}eentrypoint.sh${NORM}: Runs the Parcels Model"\\n
    show_usage
    echo -e "\n${BOLD}Required arguments:${NORM}"
    echo -e "${REV}SYNPOP_CONFIG_XML${NORM}\t the config xml from synpop"
    echo -e "${REV}TRACES${NORM}\t the traces json from the jsprit to matsim connector"
    echo -e "${REV}OUT_PATH${NORM}\t the output path"\\n
    echo -e "${BOLD}Optional arguments:${NORM}"
    echo -e "${REV}-v${NORM}\tSets verbosity level"
    echo -e "${REV}-h${NORM}\tShows this message"
    echo -e "${BOLD}Examples:${NORM}"
    echo -e "entrypoint.sh -v sample-data/input/lead_2022_5pct_config.xml sample-data/input/traces.json sample-data/output/"
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
OUT_PATH=${leftovers[2]%/}

##############################################################################
# Input checks                                                               #
##############################################################################
if [ ! -f "${SYNPOP_CONFIG_XML}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} config input file\n"; show_usage; exit 1
fi
if [ ! -f "${TRACES}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} traces input file\n"; show_usage; exit 1
fi

if [ ! -d "${OUT_PATH}" ]; then
     echo -e "Give a ${BOLD}valid${NORM} output directory\n"; show_usage; exit 1
fi

##############################################################################
# Execution                                                                  #
##############################################################################
java -Xmx20g -cp /srv/app/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
	--config-path ${SYNPOP_CONFIG_XML} \
	--output-path ${OUT_PATH}
if [ $? -ne 0 ]; then
    exit 1
fi

java -Xmx20G -cp /srv/app/java/target/lead-matsim-1.0.0.jar org.eqasim.core.scenario.cutter.RunScenarioCutter \
	--config-path ${SYNPOP_CONFIG_XML} \
	--extent-path /srv/app/data/perimeter_lyon.shp \
	--output-path ${OUT_PATH} \
	--prefix perimeter_ \
	--config:plans.inputPlansFile ${OUT_PATH}/output_plans.xml.gz
if [ $? -ne 0 ]; then
    exit 2
fi

java -Xmx20g -cp /srv/app/java/target/lead-matsim-1.0.0.jar fr.irtx.lead.matsim.RunSimulation \
    --config-path ${OUT_PATH}/perimeter_config.xml \
    --output-path ${OUT_PATH} \
    --freight-path ${TRACES}
if [ $? -ne 0 ]; then
    exit 3
fi

papermill /srv/app/src/congestion-analysis.ipynb /dev/null \
    -psimulation_output_path ${OUT_PATH} \
    -pkpi_path ${OUT_PATH}/congestion.json
