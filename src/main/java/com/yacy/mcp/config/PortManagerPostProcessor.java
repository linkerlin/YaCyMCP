package com.yacy.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortManagerPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PortManagerPostProcessor.class);

    private static final int MAX_KILL_ATTEMPTS = 3;
    private static final int WAIT_INTERVAL_MS = 1000;
    private static final int MAX_WAIT_ATTEMPTS = 10;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String portStr = environment.getProperty("server.port", "8990");
        int serverPort = parsePort(portStr);

        log.info("MCP Server port check: {}", serverPort);
        log.info("In stdio mode, MCP communication happens via stdin/stdout");
        log.info("Port {} is used for Spring Boot web server (health checks)", serverPort);

        if (isPortInUse(serverPort)) {
            log.warn("Port {} is in use, attempting to free it...", serverPort);

            List<Integer> pids = getProcessIdsUsingPort(serverPort);
            if (!pids.isEmpty()) {
                for (int pid : pids) {
                    log.info("Found process {} using port {}, attempting to terminate...", pid, serverPort);
                    boolean killed = killProcess(pid, MAX_KILL_ATTEMPTS);
                    if (killed) {
                        log.info("Successfully terminated process {}", pid);
                    } else {
                        log.warn("Failed to terminate process {} after {} attempts", pid, MAX_KILL_ATTEMPTS);
                    }
                }
            } else {
                log.debug("Could not determine PID, port might be in TIME_WAIT state");
            }

            if (waitForPortToBeFree(serverPort)) {
                log.info("Port {} is now free, server can start", serverPort);
            } else {
                log.error("Port {} is still in use after all attempts. Server may fail to start.", serverPort);
                log.error("Solutions:");
                log.error("  1. Manually kill the process using: lsof -i :{} | awk 'NR>1 {{print $2}}' | xargs kill -9", serverPort);
                log.error("  2. Change port by setting environment variable: SERVER_PORT=8991");
                log.error("  3. In MCP stdio mode, the port is only needed for health checks");
            }
        } else {
            log.debug("Port {} is free", serverPort);
        }
    }

    private int parsePort(String portStr) {
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid port '{}', using default 8990", portStr);
            return 8990;
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

    private List<Integer> getProcessIdsUsingPort(int port) {
        List<Integer> pids = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-t", "-i", ":" + String.valueOf(port));
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        pids.add(Integer.parseInt(line));
                    } catch (NumberFormatException e) {
                        log.debug("Could not parse PID from: {}", line);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.debug("Failed to get PID using lsof: {}", e.getMessage());
            tryAlternativeMethods(port, pids);
        }

        return pids;
    }

    private void tryAlternativeMethods(int port, List<Integer> pids) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-tlnp");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern pattern = Pattern.compile(".*:(\\d+)\\s+.*");

            while ((line = reader.readLine()) != null) {
                if (line.contains(":" + port)) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String[] parts = line.split("\\s+");
                        for (String part : parts) {
                            if (part.matches("\\d+/.*")) {
                                try {
                                    String pidStr = part.split("/")[0];
                                    pids.add(Integer.parseInt(pidStr));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.debug("Alternative method also failed: {}", e.getMessage());
        }
    }

    private boolean killProcess(int pid, int attempts) {
        for (int i = 1; i <= attempts; i++) {
            try {
                Process killProcess = new ProcessBuilder("kill", "-TERM", String.valueOf(pid)).start();
                int exitCode = killProcess.waitFor();

                if (exitCode == 0) {
                    Thread.sleep(500);
                    if (!isProcessRunning(pid)) {
                        return true;
                    }
                }

                if (i < attempts) {
                    log.debug("SIGTERM to process {} failed (attempt {}/{}), trying SIGKILL...", pid, i, attempts);
                    Process killProcess9 = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
                    killProcess9.waitFor();
                }
            } catch (Exception e) {
                log.debug("Failed to kill process {}: {}", pid, e.getMessage());
            }
        }
        return false;
    }

    private boolean isProcessRunning(int pid) {
        try {
            Process checkProcess = new ProcessBuilder("kill", "-0", String.valueOf(pid)).start();
            int exitCode = checkProcess.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForPortToBeFree(int port) {
        for (int attempt = 1; attempt <= MAX_WAIT_ATTEMPTS; attempt++) {
            if (!isPortInUse(port)) {
                return true;
            }
            try {
                Thread.sleep(WAIT_INTERVAL_MS);
                log.debug("Waiting for port {} to be free (attempt {}/{})", port, attempt, MAX_WAIT_ATTEMPTS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return !isPortInUse(port);
    }
}
