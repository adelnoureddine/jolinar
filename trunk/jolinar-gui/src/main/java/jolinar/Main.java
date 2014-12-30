package jolinar;/*
 * Copyright (c) 2014, Adel Noureddine.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License v3.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Author : Adel Noureddine
 */

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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
	private JTabbedPane tabbedPane1;
	private JPanel panel1;
	private JPanel monitoringPanel;
	private JPanel settingsPanel;
	private JTextField tfCommand;
	private JPanel energyResultsPanel;
	private JLabel toobalResults;
	private JTextField tfCPUTDP;
	private JTextField tfCPUFrequencies;
	private JTextField tfDiskReadPower;
	private JTextField tfDiskReadRate;
	private JTextField tfDiskWritePower;
	private JTextField tfDiskWriteRate;
	private JTextField tfMemoryReadPower;
	private JTextField tfMemoryWritePower;
	private JCheckBox tfGenerateLogs;
	private JButton saveButton;
	private JButton runButton;
	private JProgressBar progressBarCPU;
	private JProgressBar progressBarDisk;
	private JProgressBar progressBarMemory;
	private JLabel flCPUResults;
	private JLabel flDiskResults;
	private JLabel flMemoryResults;
	private JLabel flTotalEnergy;
	private JLabel tfJolinarDir;

	// Jolinar fields
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

	// Initialize logger
	ConsoleHandler ch;

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

	// Check if configuration file exist
	String homeDir = System.getProperty("user.home");
	String jolinarDir = homeDir + "/.jolinar";
	String configurationFile = homeDir + "/.jolinar/config.properties";
	String jolinarLogDir = jolinarDir + "/log";

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (UnsupportedLookAndFeelException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			// handle exception
			try {
				UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e1) {
				// handle exception
			}
		}

		Main mj = new Main();

		//ImageIcon img = new ImageIcon("img/icon.png");

		JFrame frame = new JFrame("Jolinar");
		frame.setContentPane(mj.panel1);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		//frame.setIconImage(img.getImage());
		mj.populateData();
		frame.pack();
		mj.setFocus();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private void setFocus() {
		tfCommand.requestFocus();
	}

	/**
	 * Constructor
 	 */
	public Main() {
		tfCommand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				startMonitoring();
			}
		});

		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				startMonitoring();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				saveConfiguration();
			}
		});
	}

	/**
	 * Read data from .jolinar.properties file
	 */
	private void populateData() {
		// Initialize logger
		ch = new ConsoleHandler();
		ch.setFormatter(new JolinarFormatter());
		Main.LOGGER.addHandler(ch);
		Main.LOGGER.setLevel(Level.CONFIG);
		Main.LOGGER.setUseParentHandlers(false);

		Main.LOGGER.log(Level.INFO, "Loading properties");

		File theDir = new File(jolinarDir);
		// if the directory does not exist, create it
		if (!theDir.exists()) {
			Main.LOGGER.log(Level.INFO, "Creating Jolinar directory");
			boolean result = false;
			try{
				theDir.mkdir();
			} catch(SecurityException se){
				//handle it
				Main.LOGGER.log(Level.INFO, "Can't create Jolinar directory");
			}
		}

		File theDir2 = new File(jolinarLogDir);
		// if the directory does not exist, create it
		if (!theDir2.exists()) {
			Main.LOGGER.log(Level.INFO, "Creating Jolinar directory");
			boolean result = false;
			try{
				theDir2.mkdir();
			} catch(SecurityException se){
				//handle it
				Main.LOGGER.log(Level.INFO, "Can't create Jolinar directory");
			}
		}

		// Read properties file
		readConfigFile();
	}

	private void readConfigFile() {
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

			tfCPUTDP.setText(cpuTDP.toString());
			tfCPUFrequencies.setText(cpuFrequenciesVoltages);
			tfDiskReadPower.setText(diskReadPower.toString());
			tfDiskReadRate.setText(diskReadRate.toString());
			tfDiskWritePower.setText(diskWritePower.toString());
			tfDiskWriteRate.setText(diskWriteRate.toString());
			tfMemoryReadPower.setText(memoryReadPower.toString());
			tfMemoryWritePower.setText(memoryWritePower.toString());
			tfGenerateLogs.setSelected(generateLogs);

			writeToToolbar("Ready to monitor energy");
		} catch (IOException e) {
			writeToToolbar("Error in reading settings");
		}

		// Update Jolinar directory in about panel
		tfJolinarDir.setText(jolinarDir);
	}

	/**
	 * Start monitoring application
	 */
	private void startMonitoring() {
		// Read properties file
		readConfigFile();

		String commandText = tfCommand.getText();
		String[] args = commandText.split(" ");
		if (!commandText.isEmpty() && (args.length >= 1)) {
			// Get program to monitor from argument
			programToMonitor.add(args[0]);
			if (args.length > 1) {
				for (int i = 1; i < args.length; i++) {
					programToMonitor.add(args[i]);
				}
			}

			// Fill frequencies and voltages map
			String[] cpuFrequenciesVoltagesArray = cpuFrequenciesVoltages.split(";");
			Map<Double, Double> frequenciesVoltages = new HashMap<>();
			Map<Double, Double> frequenciesMap = new HashMap<>();
			int iArray = 0;
			while (iArray < cpuFrequenciesVoltagesArray.length) {
				Double frequency = Double.valueOf(cpuFrequenciesVoltagesArray[iArray]);
				Double voltage = Double.valueOf(cpuFrequenciesVoltagesArray[iArray + 1]);
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

				if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
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
				writeToToolbar("Can't find: " + programToMonitor);
			}

			if (process != null) {
				// Run sensors and formulas
				Main.LOGGER.log(Level.INFO, "Loading energy modules");

				if (!OSValidator.isUnix()) {
					// Only Linux-based systems are supported
					Main.LOGGER.log(Level.INFO, "Only Linux-based systems are supported");
				} else {
					// Linux-based systems

					// Run CPU monitoring
					cpuSensor = new CPUSensorDVFS(appPid, frequenciesMap);
					cpuFormula = new CPUFormulaDVFS(cpuTDP, cpuTDPFactor, cpuSensor, frequenciesVoltages);
					Main.LOGGER.log(Level.INFO, "CPU module loaded");

					// Run disk monitoring
					diskSensor = new DiskSensorProc(appPid);
					diskFormula = new DiskFormulasProc(diskReadPower, diskReadRate, diskWritePower, diskWriteRate, diskSensor);
					Main.LOGGER.log(Level.INFO, "Disk module loaded");

					// Run memory monitoring
					memorySensor = new MemorySensorProc(appPid);
					memoryFormula = new MemoryFormulaProc(memoryReadPower, memoryWritePower, memorySensor);
					Main.LOGGER.log(Level.INFO, "Memory module loaded");
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
					} else {
						processDiskPower = 0.0;
						processMemoryPower = 0.0;
					}

					// Write power data to file
					if (generateLogs) {
						powerData = processCPUPower + ";" + processDiskPower + ";" + processMemoryPower + "\n";
						Main.appendToFile(jolinarLogDir + "/" + appPid + "-power.csv", powerData, true);
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
					Main.appendToFile(jolinarLogDir + "/" + appPid + "-energy.csv", energyData, true);
					Main.LOGGER.log(Level.INFO, "Power and energy data written to files " + appPid + "-power.csv and " + appPid + "-energy.csv in " + jolinarLogDir);
				}

				double totalEnergy = processCPUEnergy + processDiskEnergy + processMemoryEnergy;
				flTotalEnergy.setText("<html><body><b>" + df.format(totalEnergy) + "</b></body></html>");
				flCPUResults.setText(df.format(processCPUEnergy));
				flDiskResults.setText(df.format(processDiskEnergy));
				flMemoryResults.setText(df.format(processMemoryEnergy));

				Double perCPU = (processCPUEnergy * 100) / totalEnergy;
				Double perDisk = (processDiskEnergy * 100) / totalEnergy;
				Double perMemory = (processMemoryEnergy * 100) / totalEnergy;
				progressBarCPU.setValue(perCPU.intValue());
				progressBarCPU.setStringPainted(true);
				progressBarDisk.setValue(perDisk.intValue());
				progressBarDisk.setStringPainted(true);
				progressBarMemory.setValue(perMemory.intValue());
				progressBarMemory.setStringPainted(true);

				writeToToolbar("Monitoring ended");

				Main.LOGGER.log(Level.INFO, "Estimated energy consumption... CPU: " + df.format(processCPUEnergy) + " joules -- Disk: " + df.format(processDiskEnergy) + " joules -- Memory: " + df.format(processMemoryEnergy) + " joules");
				System.out.println();
				System.out.println("Estimated energy consumption (Joules):");
				System.out.println("----------------------------");
				System.out.println("|  " + ANSI_BOLD + "CPU" + ANSI_RESET + "   |  " + ANSI_BOLD + "Disk" + ANSI_RESET + "  | " + ANSI_BOLD + "Memory" + ANSI_RESET + " |");
				System.out.println("----------------------------");
				System.out.println("|  " + ANSI_BOLD + ANSI_YELLOW + df.format(processCPUEnergy) + ANSI_RESET + "  |  " + ANSI_BOLD + ANSI_YELLOW + df.format(processDiskEnergy) + ANSI_RESET + "  |  " + ANSI_BOLD + ANSI_YELLOW + df.format(processMemoryEnergy) + ANSI_RESET + "  |");
				System.out.println("----------------------------");
				System.out.println();

				programToMonitor.clear();
			} else {
				writeToToolbar("Error in running: " + programToMonitor);
				programToMonitor.clear();
			}

		} else {
			writeToToolbar("Program name cannot be empty");
		}
	}

	/**
	 * Write error messages
	 * @param message the message to print
	 */
	public void writeToToolbar(String message) {
		toobalResults.setText(message);
	}

	/**
	 * Save configuration to file
	 */
	public void saveConfiguration() {
		cpuTDP = Double.valueOf(tfCPUTDP.getText());
		cpuFrequenciesVoltages = tfCPUFrequencies.getText();
		diskReadPower = Double.valueOf(tfDiskReadPower.getText());
		diskReadRate = Double.valueOf(tfDiskReadRate.getText());
		diskWritePower = Double.valueOf(tfDiskWritePower.getText());
		diskWriteRate = Double.valueOf(tfDiskWriteRate.getText());
		memoryReadPower = Double.valueOf(tfMemoryReadPower.getText());
		memoryWritePower = Double.valueOf(tfMemoryWritePower.getText());
		generateLogs = tfGenerateLogs.isSelected();

		// Read properties file
		Properties prop = new Properties();
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
			saveButton.setText("Saved!");
		} catch (IOException e1) {
			Main.LOGGER.log(Level.SEVERE, e1.getMessage());
		}

		readConfigFile();
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
