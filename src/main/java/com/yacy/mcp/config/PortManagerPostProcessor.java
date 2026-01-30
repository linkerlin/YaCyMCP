package com.yacy.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
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

        log.info("MCP Server starting on port {}", serverPort);
        log.info("MCP communication via stdin/stdout, port {} for health checks", serverPort);

        if (isPortInUse(serverPort)) {
            log.warn("Port {} is in use, attempting to free it...", serverPort);

            List<Integer> pids = findProcessesUsingPort(serverPort);

            if (!pids.isEmpty()) {
                for (int pid : pids) {
                    log.info("Found process {} using port {}", pid, serverPort);
                    boolean killed = killProcess(pid, MAX_KILL_ATTEMPTS);
                    if (killed) {
                        log.info("Successfully terminated process {}", pid);
                    } else {
                        log.warn("Failed to terminate process {}", pid);
                    }
                }
            } else {
                log.debug("Could not determine PID, port might be in TIME_WAIT state");
            }

            if (waitForPortToBeFree(serverPort)) {
                log.info("Port {} is now free", serverPort);
            } else {
                log.error("Port {} is still in use. Server may fail to start.", serverPort);
            }
        } else {
            log.debug("Port {} is free", serverPort);
        }
    }

    private int parsePort(String portStr) {
        try {
            return Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
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

    private List<Integer> findProcessesUsingPort(int port) {
        List<Integer> pids = new ArrayList<>();

        if (tryLsof(port, pids)) {
            return pids;
        }

        if (tryFuser(port, pids)) {
            return pids;
        }

        if (tryNetstat(port, pids)) {
            return pids;
        }

        if (tryLsofWithEnv(port, pids)) {
            return pids;
        }

        log.debug("Could not find PID using external commands, trying /proc...");

        return pids;
    }

    private boolean tryLsof(int port, List<Integer> pids) {
        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-t", "-i", ":" + port);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            pids.add(Integer.parseInt(line));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && !pids.isEmpty()) {
                log.debug("Found PIDs using lsof: {}", pids);
                return true;
            }
        } catch (Exception e) {
            log.debug("lsof failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean tryLsofWithEnv(int port, List<Integer> pids) {
        try {
            ProcessBuilder pb = new ProcessBuilder("lsof", "-t", "-i", ":" + port);
            pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:" + System.getenv("PATH"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            pids.add(Integer.parseInt(line));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && !pids.isEmpty()) {
                log.debug("Found PIDs using lsof with env: {}", pids);
                return true;
            }
        } catch (Exception e) {
            log.debug("lsof with env failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean tryFuser(int port, List<Integer> pids) {
        try {
            ProcessBuilder pb = new ProcessBuilder("fuser", "-k", String.valueOf(port) + "/tcp");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            log.debug("fuser exit code: {}", exitCode);

            if (exitCode == 0 || exitCode == 1) {
                Thread.sleep(500);
                if (!isPortInUse(port)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("fuser failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean tryNetstat(int port, List<Integer> pids) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-tlnp");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String searchStr = ":" + port + " ";

                while ((line = reader.readLine()) != null) {
                    if (line.contains(searchStr)) {
                        Pattern pattern = Pattern.compile("(\\d+)/");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            try {
                                pids.add(Integer.parseInt(matcher.group(1)));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (!pids.isEmpty()) {
                log.debug("Found PIDs using netstat: {}", pids);
                return true;
            }
        } catch (Exception e) {
            log.debug("netstat failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean killProcess(int pid, int attempts) {
        for (int i = 1; i <= attempts; i++) {
            try {
                if (!isProcessRunning(pid)) {
                    return true;
                }

                Process killProcess = new ProcessBuilder("kill", "-TERM", String.valueOf(pid)).start();
                int exitCode = killProcess.waitFor();

                if (exitCode == 0) {
                    Thread.sleep(500);
                    if (!isProcessRunning(pid)) {
                        return true;
                    }
                }

                if (i < attempts) {
                    log.debug("SIGTERM to process {} failed, trying SIGKILL...", pid);
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return !isPortInUse(port);
    }
}
