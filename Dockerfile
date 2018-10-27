FROM ubuntu:18.04

LABEL maintainer="contact@securify.ch"

# install basic packages
RUN apt-get update && apt-get install -y\
    software-properties-common\
    locales

# set correct locale
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8


# install souffle
RUN apt-get update &&\
        apt-get -y install\
        wget\
        gdebi

RUN mkdir /souffle
RUN wget -P /souffle/ https://github.com/souffle-lang/souffle/releases/download/1.4.0/souffle_1.4.0-1_amd64.deb &&\
        gdebi --n /souffle/souffle_1.4.0-1_amd64.deb

# install java and pip
RUN apt-get update && apt-get -y install\
        openjdk-8-jdk\
        python3-pip

RUN pip3 install --user py-solc termcolor

# install truffle for project compilation
RUN apt-get update && apt-get install -y nodejs\
                npm

# install truffle v5-beta
RUN npm install -g truffle@beta

# copy and compile securify
COPY . /sec

WORKDIR /sec

RUN ./gradlew jar

RUN mkdir /securify_jar

RUN cp build/libs/*.jar /securify_jar/securify.jar

RUN mkdir -p /smt_files
COPY ./smt_files/* /smt_files/

# Solidity example
COPY src/test/resources/solidity/transaction-reordering.sol /project/example.sol

# Copy python scripts
RUN mkdir -p /scripts
COPY ./scripts/* /scripts/

COPY docker_run_securify.py /

WORKDIR /

# ENTRYPOINT allows arguments to be passed (e.g. "--truffle").
ENTRYPOINT ["python3", "-O", "docker_run_securify.py"]
