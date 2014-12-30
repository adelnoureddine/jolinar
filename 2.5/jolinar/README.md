# Jolinar 2

Jolinar is a Java program for monitoring the power consumption of applications at the process level.
It monitors the energy consumption of an application (its main process, through its PID).

## Documentation
* [Getting started](#getting-started)
* [Future works](#future-works)
* [License](#license)

## Getting started

Jolinar is fully written in Java (v. 7+) and its project is fully managed by [Maven](http://maven.apache.org "Maven") (v. 3).

### How to use it

Simple run the provided embedded executable, while providing the program to monitar as a command line argument:

```bash
./$JOLINAR programToMonitor
```

Jolinar will run the program to monitor and monitor its energy consumption, then generate results at the program's exit.
If you want to submit arguments to your monitored program, then you can do as usual, just add them on the command line as follows:

```bash
./$JOLINAR programToMonitor arg1 arg2 ...
```

Note that you only need the `jolinar` executable.
The jar is file already embedded inside the executable shell script `$JOLINAR.run`. 
However, if you prefer to use the jar, you can simply run:

```bash
java -jar $JOLINAR.jar programToMonitor
```

### How to configure it

Configuring Jolinar is achieved by changing configuration options directly from the Jolinar program.
Jolinar will try to read the `.jolinar.properties` file in the user's home directory.
If the file is not found, the user have the choice to let Jolinar create a new file (while inputing the required hardware data).
Jolinar's configuration can be modified by changing Jolinar's flags. For example:

```bash
$JOLINAR -tdp 35
```

This will set the CPU TDP to 35 Watts and save the data in the configuration file.
A list of options and flags is accessible by simply typing:

```bash
$JOLINAR -h
```

### How to build it

In order to build and use Jolinar, download the source code by cloning it via your Git client:

```bash
git clone git://github.com/adelnoureddine/jolinar.git
```

Jolinar uses [Maven](http://maven.apache.org "Maven") to manage the project, therefore your need to compile the Java source file and generate the Jolinar.jar archive by typying the following commands:

```bash
cd $JOLINAR_DIR
mvn clean install
```

A `build.sh` file is also provided to automate the compilation and generation of Jolinar by using:
```bash
cd $JOLINAR_DIR
sh build.sh
```

A `release.sh` file is also provided to automate the compilation and generation of Jolinar, and embedding it into a Linux executable. Simple use:
```bash
cd $JOLINAR_DIR
sh release.sh
```

A `$JOLINAR` executable file will be generated and its executable flag set to true.
This file is self-sufficient, you won't need the jar file anymore in order to use Jolinar.

### Generated files

When Jolinar starts monitoring an application at runtime, and if logs are enabled, it generated two CSV files named `PID-power.csv` and `PID-energy.csv`.
The format of `PID-power.csv` is as follows: `CPU-Power;Disk-Power;Memory-Power` where CPU-Power is the CPU power of the process, Disk-Power is the disk power, and Memory-Power is the memory power.
The file will outline the power consumption of the program with an interval of 500 milliseconds.
The format of `PID-energy.csv` is similar but with the total energy consumption of the program during its execution.
Note that energy values are calculated on runtime, with the same 500 ms interval, then their sum is written to the file.
You can disable generating files by changing the settings in the `config.properties` file.
In this case, Jolinar will only display the total energy consumption on the terminal.

## Future works

We are working on adding new _energy models_ for estimating the energy consumption of software by other hardware resources, and also improving the source code and our software.

If you are interested in joining, feel free to contact us via our [GitHub](https://github.com/adelnoureddine/jolinar "GitHub") webpage or email us at adel.noureddine@outlook.com!

## License

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