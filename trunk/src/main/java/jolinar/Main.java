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
import jolinar.formulas.cpu.CPUFormulaMaxFrequency;
import jolinar.formulas.cpu.CPUFormulasInterface;
import jolinar.formulas.disk.DiskFormulasInterface;
import jolinar.formulas.disk.DiskFormulasProc;
import jolinar.sensors.cpu.CPUSensorDVFS;
import jolinar.sensors.cpu.CPUSensorSigarMaxFrequency;
import jolinar.sensors.cpu.CPUSensorsInterface;
import jolinar.sensors.disk.DiskSensorProc;
import jolinar.sensors.disk.DiskSensorsInterface;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {
		System.out.println("+-----------------------------------+");
		System.out.println("| Jolinar Version 0.1               |");
		System.out.println("+-----------------------------------+");

		// Initialize logger
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(new JolinarFormatter());
		Main.LOGGER.addHandler(ch);
		Main.LOGGER.setLevel(Level.CONFIG);
		Main.LOGGER.setUseParentHandlers(false);


		String userDir = System.getProperty("user.dir");
		int appPid;

		// Sensors and formulas
		CPUSensorsInterface cpuSensor;
		CPUFormulasInterface cpuFormula;
		DiskSensorsInterface diskSensor;
		DiskFormulasInterface diskFormula = null;

		// Hardware information
		Double cpuTDP, cpuTDPFactor = 0.7;
		String cpuFrequenciesVoltages;
		Double diskReadPower, diskReadRate, diskWritePower, diskWriteRate;

		String powerData = "";

		Main.LOGGER.log(Level.INFO, "Loading properties");

		// Read properties file
		Properties prop = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("./config.properties");
			prop.load(fis);
		} catch (IOException e) {
			Main.LOGGER.log(Level.SEVERE, "No config.properties file found in current directory: " + userDir);
			System.exit(1);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}
		}

		// Hardware data
		cpuTDP = Double.valueOf(prop.getProperty("cpu-tdp"));
		cpuFrequenciesVoltages = prop.getProperty("cpu-frequencies-voltages");
		diskReadPower = Double.valueOf(prop.getProperty("disk-read-power"));
		diskReadRate = Double.valueOf(prop.getProperty("disk-read-rate"));
		diskWritePower = Double.valueOf(prop.getProperty("disk-write-power"));
		diskWriteRate = Double.valueOf(prop.getProperty("disk-write-rate"));

		// PID
		if (args.length == 1)
			appPid = Integer.parseInt(args[0]);
		else
			appPid = Integer.valueOf(prop.getProperty("app-pid"));

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

		Main.addToJavaLibraryPath(new File(System.getProperty("user.dir") + "/lib/"));


		// Run sensors and formulas
		Main.LOGGER.log(Level.INFO, "Loading energy modules");

		// CPU
		if (! OSValidator.isUnix()) {
			// All systems except Linux-based
			cpuSensor = new CPUSensorSigarMaxFrequency(appPid);
			cpuFormula = new CPUFormulaMaxFrequency(cpuTDP, cpuTDPFactor, cpuSensor);

			Main.LOGGER.log(Level.INFO, "CPU...OK -- Max frequency");
			Main.LOGGER.log(Level.WARNING, "Disk...Fail -- Only supported on Linux-based systems");
		} else {
			// Linux-based systems
			try {
				// First attempt to use DVFS
				cpuSensor = new CPUSensorDVFS(appPid, frequenciesMap);
				cpuFormula = new CPUFormulaDVFS(cpuTDP, cpuTDPFactor, cpuSensor, frequenciesVoltages);
				Main.LOGGER.log(Level.INFO, "CPU...OK -- DVFS");
			} catch (Exception e) {
				// Fail to use DVFS, then use max frequency
				cpuSensor = new CPUSensorSigarMaxFrequency(appPid);
				cpuFormula = new CPUFormulaMaxFrequency(cpuTDP, cpuTDPFactor, cpuSensor);
				Main.LOGGER.log(Level.INFO, "CPU...OK -- Max frequency");
			}

			// Run disk monitoring
			// Linux-based systems only
			diskSensor = new DiskSensorProc(appPid);
			diskFormula = new DiskFormulasProc(diskReadPower, diskReadRate, diskWritePower, diskWriteRate, diskSensor);

			Main.LOGGER.log(Level.INFO, "Disk...OK");
		}


		// Start runtime monitoring of application
		Main.LOGGER.log(Level.INFO, "Started monitoring application with ID " + appPid);

		while (true) { // While loop for application cycle
			Double processCPUPower = 0.0, processDiskPower = 0.0;
			processCPUPower = cpuFormula.getCPUPower();

			if (OSValidator.isUnix())
				processDiskPower = diskFormula.getCPUPower();
			else
				processDiskPower = 0.0;

			// Write data to file
			powerData = processCPUPower + ";" + processDiskPower + "\n";
			Main.appendToFile(userDir + "/" + appPid + ".csv", powerData, true);

			try {
				Thread.sleep(500); // Sleep for 500 ms
			} catch (InterruptedException e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}

		}

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
	 * Add a new folder to java.library.path
	 * @param dir The new folder to add
	 */
	public static void addToJavaLibraryPath(File dir) {
		final String libraryPath = "java.library.path";
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(dir + " is not a directory.");
		}
		String javaLibraryPath = System.getProperty(libraryPath);
		System.setProperty(libraryPath, javaLibraryPath + File.pathSeparatorChar + dir.getAbsolutePath());

		resetJavaLibraryPath();
	}

	/**
	 * Delete java.library.path cache.
	 * Therefore the classloader is forced to recheck its value at the next library loading.
	 * Only supported for Oracle's JVM
	 */
	public static void resetJavaLibraryPath() {
		synchronized(Runtime.getRuntime()) {
			try {
				Field field = ClassLoader.class.getDeclaredField("usr_paths");
				field.setAccessible(true);
				field.set(null, null);

				field = ClassLoader.class.getDeclaredField("sys_paths");
				field.setAccessible(true);
				field.set(null, null);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				Main.LOGGER.log(Level.WARNING, e.getMessage());
			}
		}
	}
}