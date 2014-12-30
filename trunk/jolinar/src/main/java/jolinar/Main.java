/*
 * Copyright (c) 2014, Inria, University Lille 1.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License v3.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Author : Adel Noureddine
 */

package jolinar;

import jolinar.formulas.cpu.CPUFormulaDVFS;
import jolinar.formulas.cpu.CPUFormulasInterface;
import jolinar.formulas.disk.DiskFormulasInterface;
import jolinar.formulas.disk.DiskFormulasProc;
import jolinar.formulas.memory.MemoryFormulaProc;
import jolinar.formulas.memory.MemoryFormulasInterface;
import jolinar.sensors.cpu.CPUSensorDVFS;
import jolinar.sensors.cpu.CPUSensorsInterface;
import jolinar.sensors.disk.DiskSensorProc;
import jolinar.sensors.disk.DiskSensorsInterface;
import jolinar.sensors.memory.MemorySensorProc;
import jolinar.sensors.memory.MemorySensorsInterface;

import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_BOLD = "\u001B[1m";

	public static void main(String[] args) {
		System.out.println(ANSI_BOLD + "~~" + ANSI_YELLOW + " Jolinar 2 " + ANSI_RESET + ANSI_BOLD + "~~" + ANSI_RESET);

		// Initialize logger
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new JolinarFormatter());
		Main.LOGGER.addHandler(ch);
		Main.LOGGER.setLevel(Level.CONFIG);
		Main.LOGGER.setUseParentHandlers(false);

		String userDir = System.getProperty("user.dir");
		int appPid = 0;
		List<String> programToMonitor = new ArrayList<String>();
		boolean generateLogs = true;

		// Sensors and formulas
		CPUSensorsInterface cpuSensor;
		CPUFormulasInterface cpuFormula = null;
		DiskSensorsInterface diskSensor;
		DiskFormulasInterface diskFormula = null;
		MemorySensorsInterface memorySensor;
		MemoryFormulasInterface memoryFormula = null;

		// Hardware information
		Double cpuTDP = 0.0, cpuTDPFactor = 0.7;
		String cpuFrequenciesVoltages = "";
		Double diskReadPower = 0.0, diskReadRate = 0.0, diskWritePower = 0.0, diskWriteRate = 0.0;
		Double memoryReadPower = 0.0, memoryWritePower = 0.0;

		String powerData = "";

		Main.LOGGER.log(Level.INFO, "Loading properties");

		// Check if configuration file exist
		String homeDir = System.getProperty("user.home");
		String configurationFile = homeDir + "/.jolinar.properties";

		// Read properties file
		Properties prop = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(configurationFile);
			prop.load(fis);

			// Hardware data
			cpuTDP = Double.valueOf(prop.getProperty("cpu-tdp"));
			cpuFrequenciesVoltages = prop.getProperty("cpu-frequencies-voltages");
			diskReadPower = Double.valueOf(prop.getProperty("disk-read-power"));
			diskReadRate = Double.valueOf(prop.getProperty("disk-read-rate"));
			diskWritePower = Double.valueOf(prop.getProperty("disk-write-power"));
			diskWriteRate = Double.valueOf(prop.getProperty("disk-write-rate"));
			memoryReadPower = Double.valueOf(prop.getProperty("memory-read-power"));
			memoryWritePower = Double.valueOf(prop.getProperty("memory-write-power"));
			generateLogs = Boolean.valueOf(prop.getProperty("generate-logs"));
		} catch (IOException e) {
			// File not found, attempt to create one
			Main.LOGGER.log(Level.WARNING, "No properties file found in home directory: " + configurationFile);
			System.out.print("Create a new properties file? (Y/n) ");
			Scanner input = new Scanner(System.in);
			//String answer = input.next();
			String answer = input.nextLine();
			if (answer.equals("") || answer.equalsIgnoreCase("y")) {
				// Create properties file
				System.out.println("Jolinar requires some hardware related information. Please type in the required data:");
				System.out.print("\nCPU TDP: ");
				cpuTDP = input.nextDouble();
				System.out.println("\nCPU frequencies and voltages (format: frequency;voltage;frequency;voltage): ");
				cpuFrequenciesVoltages = input.next();
				System.out.print("\nDisk read power in Watts: ");
				diskReadPower = input.nextDouble();
				System.out.print("\nDisk read rate in MB/s: ");
				diskReadRate = input.nextDouble();
				System.out.print("\nDisk write power in Watts: ");
				diskWritePower = input.nextDouble();
				System.out.print("\nDisk write rate in MB/s: ");
				diskWriteRate = input.nextDouble();
				System.out.print("\nMemory read power in Watts: ");
				memoryReadPower = input.nextDouble();
				System.out.print("\nMemory write power in Watts: ");
				memoryWritePower = input.nextDouble();
				System.out.print("\nGenerate logs? (true, false): ");
				generateLogs = input.nextBoolean();
				System.out.println();

				prop.setProperty("cpu-tdp", cpuTDP.toString());
				prop.setProperty("cpu-frequencies-voltages", cpuFrequenciesVoltages);
				prop.setProperty("disk-read-power", diskReadPower.toString());
				prop.setProperty("disk-read-rate", diskReadRate.toString());
				prop.setProperty("disk-write-power", diskWritePower.toString());
				prop.setProperty("disk-write-rate", diskWriteRate.toString());
				prop.setProperty("memory-read-power", memoryReadPower.toString());
				prop.setProperty("memory-write-power", memoryWritePower.toString());
				prop.setProperty("generate-logs", String.valueOf(generateLogs));

				File f = new File(configurationFile);
				OutputStream out = null;
				try {
					out = new FileOutputStream(f);
					prop.store(out, "# Jolinar 2 configuration file");
				} catch (IOException e1) {
					Main.LOGGER.log(Level.SEVERE, e1.getMessage());
					System.exit(1);
				}
			} else {
				Main.LOGGER.log(Level.SEVERE, "Jolinar requires a configuration file in order to run");
				System.exit(1);
			}
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}
		}

		// Get program to monitor from argument
		if (args.length >= 1) {
			if (args[0].startsWith("-")) {
				Main.LOGGER.log(Level.INFO, "Jolinar configurator");
				switch (args[0]) {
					case "-l":
						changeProperty(configurationFile, "generate-logs", String.valueOf(args[1]));
						break;
					case "-tdp":
						changeProperty(configurationFile, "cpu-tdp", String.valueOf(args[1]));
						break;
					case "-drp":
						changeProperty(configurationFile, "disk-read-power", String.valueOf(args[1]));
						break;
					case "-drr":
						changeProperty(configurationFile, "disk-read-rate", String.valueOf(args[1]));
						break;
					case "-dwp":
						changeProperty(configurationFile, "disk-write-power", String.valueOf(args[1]));
						break;
					case "-dwr":
						changeProperty(configurationFile, "disk-write-rate", String.valueOf(args[1]));
						break;
					case "-mrp":
						changeProperty(configurationFile, "memory-read-power", String.valueOf(args[1]));
						break;
					case "-mwp":
						changeProperty(configurationFile, "disk-write-power", String.valueOf(args[1]));
						break;
					case "-h":
						Main.LOGGER.log(Level.INFO, "Available options");
						System.out.println("-l\tGenerated logs");
						System.out.println("-tdp\tCPU TDP");
						System.out.println("-drp\tDisk read power");
						System.out.println("-drr\tDisk read rate");
						System.out.println("-dwp\tDisk write power");
						System.out.println("-dwr\tDisk write rate");
						System.out.println("-mrp\tmemory read power");
						System.out.println("-mwp\tmemory write power");
						System.out.println("-h\tHelp");
						break;
					default:
						Main.LOGGER.log(Level.INFO, "Available options:");
						System.out.println("-l\tGenerated logs");
						System.out.println("-tdp\tCPU TDP");
						System.out.println("-drp\tDisk read power");
						System.out.println("-drr\tDisk read rate");
						System.out.println("-dwp\tDisk write power");
						System.out.println("-dwr\tDisk write rate");
						System.out.println("-mrp\tmemory read power");
						System.out.println("-mwp\tmemory write power");
						System.out.println("-h\tHelp");
						break;
				}
				System.exit(0);
			} else {
				programToMonitor.add(args[0]);
				if (args.length > 1) {
					for (int i = 1; i < args.length; i++) {
						programToMonitor.add(args[i]);
					}
				}
			}
		}
		else {
			Main.LOGGER.log(Level.SEVERE, "Jolinar requires to set the program to monitor as argument");
			System.exit(1);
		}

		// Fill frequencies and voltages map
		String[] cpuFrequenciesVoltagesArray = cpuFrequenciesVoltages.split(";");
		Map<Double, Double> frequenciesVoltages = new HashMap<>();
		Map<Double, Double> frequenciesMap = new HashMap<>();
		int iArray = 0;
		while (iArray < cpuFrequenciesVoltagesArray.length) {
			Double frequency = Double.valueOf(cpuFrequenciesVoltagesArray[iArray]);
			Double voltage = Double.valueOf(cpuFrequenciesVoltagesArray[iArray+1]);
			frequenciesVoltages.put(frequency, voltage);
			frequenciesMap.put(frequency, 0.0);
			iArray += 2;
		}

		// Launch program to monitor from command line
		Process process = null;
		try {
			ProcessBuilder pb = new ProcessBuilder(programToMonitor);
			// Redirect error stream to output stream
			pb.redirectErrorStream(true);
			// Redirect output stream to standard output
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			// Start the process
			process = pb.start();

			if(process.getClass().getName().equals("java.lang.UNIXProcess")) {
  				/* get the PID on unix/linux systems */
				try {
					Field f = process.getClass().getDeclaredField("pid");
					f.setAccessible(true);
					appPid = f.getInt(process);
				} catch (Exception e) {
					Main.LOGGER.log(Level.WARNING, e.getMessage());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


		// Run sensors and formulas
		Main.LOGGER.log(Level.INFO, "Loading energy modules");

		if (! OSValidator.isUnix()) {
			// Only Linux-based systems are supported
			Main.LOGGER.log(Level.SEVERE, "Only Linux-based systems aare supported");
		} else {
			// Linux-based systems

			// Run CPU monitoring
			cpuSensor = new CPUSensorDVFS(appPid, frequenciesMap);
			cpuFormula = new CPUFormulaDVFS(cpuTDP, cpuTDPFactor, cpuSensor, frequenciesVoltages);
			Main.LOGGER.log(Level.INFO, "CPU...OK");

			// Run disk monitoring
			diskSensor = new DiskSensorProc(appPid);
			diskFormula = new DiskFormulasProc(diskReadPower, diskReadRate, diskWritePower, diskWriteRate, diskSensor);
			Main.LOGGER.log(Level.INFO, "Disk...OK");

			// Run memory monitoring
			memorySensor = new MemorySensorProc(appPid);
			memoryFormula = new MemoryFormulaProc(memoryReadPower, memoryWritePower, memorySensor);
			Main.LOGGER.log(Level.INFO, "Memory...OK");
		}


		// Start runtime monitoring of application
		Main.LOGGER.log(Level.INFO, "Started monitoring application with PID " + appPid);

		// Check if monitored program process still exists
		boolean hasExisted = false;
		Field fe = null;
		try {
			fe = process.getClass().getDeclaredField("hasExited");
			fe.setAccessible(true);
			hasExisted = fe.getBoolean(process);
		} catch (Exception e) {
			Main.LOGGER.log(Level.WARNING, e.getMessage());
		}

		Double processCPUEnergy = 0.0, processDiskEnergy = 0.0, processMemoryEnergy = 0.0;
		while (!hasExisted) { // While loop for application cycle
			Double processCPUPower = 0.0, processDiskPower = 0.0, processMemoryPower = 0.0;
			processCPUPower = cpuFormula.getCPUPower();

			if (OSValidator.isUnix()) {
				processDiskPower = diskFormula.getDiskPower();
				processMemoryPower = memoryFormula.getMemoryPower();
			}
			else {
				processDiskPower = 0.0;
				processMemoryPower = 0.0;
			}

			// Write power data to file
			if (generateLogs) {
				powerData = processCPUPower + ";" + processDiskPower + ";" + processMemoryPower + "\n";
				Main.appendToFile(userDir + "/" + appPid + "-power.csv", powerData, true);
			}

			// Calculate energy consumption for each cycle (0.5 seconds)
			processCPUEnergy += (processCPUPower * 0.5);
			processDiskEnergy += (processDiskPower * 0.5);
			processMemoryEnergy += (processMemoryPower * 0.5);

			try {
				Thread.sleep(500); // Sleep for 500 ms
			} catch (InterruptedException e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}

			try {
				hasExisted = fe.getBoolean(process);
			} catch (Exception e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}
		}

		// Generate energy values and write to file
		DecimalFormat df = new DecimalFormat("0.00");

		if (generateLogs) {
			String energyData = processCPUEnergy + ";" + processDiskEnergy + ";" + processMemoryEnergy + "\n";
			Main.appendToFile(userDir + "/" + appPid + "-energy.csv", energyData, true);
			Main.LOGGER.log(Level.INFO, "Power and energy data written to files " + appPid + "-power.csv and " + appPid + "-energy.csv in " + userDir);
		}

		Main.LOGGER.log(Level.INFO, "Estimated energy consumption... CPU: " + df.format(processCPUEnergy) + " joules -- Disk: " + df.format(processDiskEnergy) + " joules -- Memory: " + df.format(processMemoryEnergy) + " joules");
		System.out.println();
		System.out.println("Estimated energy consumption (Joules):");
		System.out.println("----------------------------");
		System.out.println("|  " + ANSI_BOLD + "CPU" + ANSI_RESET + "   |  " + ANSI_BOLD + "Disk" + ANSI_RESET + "  | " + ANSI_BOLD + "Memory" + ANSI_RESET + " |");
		System.out.println("----------------------------");
		System.out.println("|  " + ANSI_BOLD + ANSI_YELLOW + df.format(processCPUEnergy) + ANSI_RESET + "  |  " + ANSI_BOLD + ANSI_YELLOW + df.format(processDiskEnergy) + ANSI_RESET + "  |  " + ANSI_BOLD + ANSI_YELLOW + df.format(processMemoryEnergy) + ANSI_RESET + "  |");
		System.out.println("----------------------------");
		System.out.println();
	}

	/**
	 * Append string to file
	 * @param fileName filename to add string to
	 * @param methData the string data to add
	 * @param append true to append to file, false to replace file with new data
	 */
	public static void appendToFile(String fileName, String methData, Boolean append) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, append));
			out.write(methData);
			out.close();
		} catch (IOException e) {
			Main.LOGGER.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * Change properties file key value
	 * @param filename Configuration.properties file
	 * @param key the key to change
	 * @param value the new value for key
	 * @throws IOException
	 */
	public static void changeProperty(String filename, String key, String value) {
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(filename));
			prop.setProperty(key, value);
			prop.store(new FileOutputStream(filename),null);
			Main.LOGGER.log(Level.INFO, "Change saved for " + key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}