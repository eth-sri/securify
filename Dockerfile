FROM ubuntu:18.10

LABEL maintainer="contact@chainsecurity.com"

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

RUN wget https://github.com/souffle-lang/souffle/releases/download/1.5.1/souffle_1.5.1-1_amd64.deb -O /tmp/souffle.deb &&\
        gdebi --n /tmp/souffle.deb

# install java and pip
RUN apt-get update && apt-get -y install\
        openjdk-8-jdk\
        python3-pip

COPY requirements.txt /tmp/
RUN pip3 install --user -r /tmp/requirements.txt

# install truffle for project compilation
RUN apt-get update && apt-get install -y\
      nodejs\
      npm

ARG truffle="latest"
RUN npm install -g truffle@$truffle

WORKDIR /sec

# To cache gradle distribution
COPY gradlew settings.gradle /sec/
COPY gradle /sec/gradle/
RUN ./gradlew -v

# copy and compile securify
COPY . /sec

RUN ./gradlew jar

# Solidity example
COPY src/test/resources/solidity/transaction-reordering.sol /project/example.sol

# this Python script allows arguments to be passed (e.g. "--truffle").
ENTRYPOINT ["python3", "docker_run_securify.py", "-p", "/project"]
