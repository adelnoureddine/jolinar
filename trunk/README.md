# Jolinar

Jolinar is a Java program for monitoring the power consumption of applications at the process level.
It monitors the energy consumption of processes by their PID at runtime.

## Documentation
* [Getting started](#getting-started)
* [Future works](#future-works)
* [License](#license)

<h2 id="getting-started">Getting started</h2>

Jolinar is fully written in Java (v. 7+) and its project is fully managed by [Maven](http://maven.apache.org "Maven") (v. 3).

### How to install it

In order to install and use Jolinar, download the source code by cloning it via your Git client:

```bash
git clone git://github.com/adelnoureddine/jolinar.git
```

Jolinar uses [Maven](http://maven.apache.org "Maven") to manage the project, therefore your need to compile the Java source file and generate the Jolinar.jar archive by typying the following commands:

```bash
cd $JOLINAR_DIR
mvn clean package -Pstandalone
```

A `build.sh` file is also provided to automate the compilation and generation of Jolinar by using:
```bash
cd $JOLINAR_DIR
sh buil.sh
```

### How to use it

Simple, run the program jar while providing the process PID to monitar as command line argument, or in the configuration file :

```bash
java -jar $JOLINAR.jar PID
```

### How to configure it

Configuring Jolinar is achieved by modifying the `config.properties` file.

### Generated files

When Jolinar starts monitoring an application at runtime, it generated one CSV file named `PID.csv`.
The format of the file is as follows: `CPU-Power;Disk-Power` where CPU-Power is the CPU power of the process and Disk-Power is the disk power.

<h2 id="future-works">Future works</h2>

We are working on adding new _energy models_ for estimating the energy consumption of software by other hardware resources. If you are interested in joining, feel free to contact us via our [GitHub](https://github.com/adelnoureddine/jolinar "GitHub") webpage or email us at adel.noureddine@inria.fr!

<h2 id="license">License</h2>

Copyright (c) 2014, Inria, University Lille 1.

Jolinar is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Jolinar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Jolinar. If not, see <http://www.gnu.org/licenses/>.