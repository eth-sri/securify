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

RUN pip3 install --user py-solc

# install solc
RUN wget https://github.com/ethereum/solidity/releases/download/v0.4.24/solc-static-linux -O /usr/local/bin/solc &&\
  chmod u+x /usr/local/bin/solc

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

COPY docker_run_securify /

RUN chmod u+x /docker_run_securify

WORKDIR /

RUN python3 -m sec.scripts.install_solc

CMD ["/docker_run_securify"]
