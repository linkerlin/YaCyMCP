package com.yacy.mcp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class PortManagerPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        int serverPort = Integer.parseInt(environment.getProperty("server.port", "8990"));

        System.out.println("[PortManager] Checking port " + serverPort + "...");

        if (isPortInUse(serverPort)) {
            int pid = getProcessIdUsingPort(serverPort);
            if (pid > 0) {
                System.out.println("[PortManager] Port " + serverPort + " is in use by process " + pid + ". Terminating...");
                if (killProcess(pid)) {
                    System.out.println("[PortManager] Successfully terminated process " + pid);
                    waitForPortToBeFree(serverPort);
                } else {
                    System.out.println("[PortManager] Failed to terminate process " + pid);
                }
            }
        } else {
            System.out.println("[PortManager] Port " + serverPort + " is free");
        }
    }

    private boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private int getProcessIdUsingPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-t", "-i", ":" + port);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            
            if (line != null && !line.isEmpty()) {
                return Integer.parseInt(line.trim());
            }
        } catch (Exception e) {
            System.out.println("[PortManager] Failed to get PID: " + e.getMessage());
        }
        return -1;
    }

    private boolean killProcess(int pid) {
        try {
            Process killProcess = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
            int exitCode = killProcess.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.out.println("[PortManager] Failed to kill: " + e.getMessage());
            return false;
        }
    }

    private void waitForPortToBeFree(int port) {
        int attempts = 0;
        while (attempts < 10 && isPortInUse(port)) {
            try {
                Thread.sleep(500);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (isPortInUse(port)) {
            System.out.println("[PortManager] Port " + port + " still in use after " + attempts + " attempts");
        } else {
            System.out.println("[PortManager] Port " + port + " is now free");
        }
    }
}
