/*
 * Copyright (c) 2014, Adel Noureddine.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License v3.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Author : Adel Noureddine
 */

package jolinar.sensors.memory;

import jolinar.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

public class MemorySensorProc implements MemorySensorsInterface {

	/**
	 * Process PID to monitor
	 */
	private int pid;

	/**
	 * Path to the io file where total memory data is stored
	 * Linux-systems only
	 */
	private String totalMemoryIOPath;

	/**
	 * Path to the io file where process PID memory data is stored
	 * Linux-systems only
	 */
	private String pidMemoryIOPath;

	/**
	 * Total memory of the system
	 */
	private int totalMemory;

	/**
	 * Constructor
	 * @param pid Process PID to monitor
	 */
	public MemorySensorProc(int pid) {
		this.pid = pid;
		this.totalMemoryIOPath = "/proc/meminfo";
		this.pidMemoryIOPath = "/proc/" + pid + "/status";
		this.populateMemorySensor();
	}

	private void populateMemorySensor() {
		try {
			List<String> allLines = Files.readAllLines(Paths.get(this.totalMemoryIOPath), StandardCharsets.UTF_8);
			for (String line : allLines) {
				// Check for total system memory
				if (line.startsWith("MemTotal")) {
					// Line is similar to: MemTotal:        3923808 kB
					this.totalMemory = Integer.valueOf(line.split(":")[1].trim().split(" ")[0]);
					break;
				}
			}
		} catch (IOException e) {
			Main.LOGGER.log(Level.WARNING, e.getMessage());
		}
	}

	@Override
	public double getProcesMemoryPercentage() {
		if (this.totalMemory == 0)
			return 0;

		double pidMemory = 0;

		try {
			List<String> allLines = Files.readAllLines(Paths.get(this.pidMemoryIOPath), StandardCharsets.UTF_8);
			for (String line : allLines) {
				// Check for resident set size
				if (line.startsWith("VmRSS")) {
					// Line is similar to: VmRSS:	    6740 kB
					pidMemory = Integer.valueOf(line.split(":")[1].trim().split(" ")[0]);
					break;
				}
			}
		} catch (IOException e) {
			Main.LOGGER.log(Level.WARNING, e.getMessage());
		}

		return (pidMemory / this.totalMemory);
	}

}
