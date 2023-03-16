FROM ubuntu:22.04

RUN apt-get update \
    && apt-get install -y wget python3.9 build-essential \
    && apt-get clean && apt-get autoclean && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/*

RUN cd /tmp \
    && wget https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.9.1%2B1/OpenJDK11U-jdk_x64_linux_hotspot_11.0.9.1_1.tar.gz -O java.tar.gz \
    && mkdir $HOME/java \
    && tar -xf java.tar.gz -C $HOME/java \
    && wget http://mirror.easyname.ch/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -O maven.tar.gz \
    && mkdir $HOME/maven \
    && tar -xf maven.tar.gz -C $HOME/maven \
    && wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O miniconda.sh \
    && bash miniconda.sh -b -p $HOME/miniconda

ENV PATH=/root/miniconda/bin:/root/osmosis/bin:/root/java/jdk-11.0.9.1+1/bin:/root/maven/apache-maven-3.6.3/bin:$PATH
COPY environment.yml /srv/app/

RUN . "/root/miniconda/etc/profile.d/conda.sh" \
    && conda config --set always_yes yes --set changeps1 no \
    && conda update -q conda \
    && conda env create -f /srv/app/environment.yml

ENV PATH=/root/miniconda/envs/matsim/bin:$PATH

RUN python3 --version && which python3 \
    && java -version && which java \
    && mvn -v && which mvn \
    && echo $PATH

COPY ./java/src /srv/app/java/src
COPY ./java/pom.xml /srv/app/java/
RUN cd /srv/app/java \
    && mvn package \
    && cd -

ENV TERM=xterm
COPY src /srv/app/src
COPY data /srv/app/data
RUN chmod +x /srv/app/src/entrypoint.sh

ENTRYPOINT [ "/srv/app/src/entrypoint.sh" ]
