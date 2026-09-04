package com.abosultan.darbaksystem.bridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

final class RootShell {
    private static final long TIMEOUT_MS = 8000L;

    private RootShell() {
    }

    static boolean hasSuBinary() {
        String[] paths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/vendor/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        ShellResult result = runNormal("command -v su || which su");
        return result.isSuccess() && result.output.contains("su");
    }

    static ShellResult checkRoot() {
        return runRoot("id");
    }

    static ShellResult enableTemporaryAdb() {
        String command =
                "setprop service.adb.tcp.port 5555; " +
                "setprop ctl.restart adbd; sleep 3; " +
                "echo tcp_port=$(getprop service.adb.tcp.port); " +
                "echo adbd=$(getprop init.svc.adbd); " +
                "echo user=$(id)";
        return runNormal(command);
    }

    static ShellResult disableTemporaryAdb() {
        String command =
                "setprop service.adb.tcp.port -1; " +
                "setprop ctl.restart adbd; sleep 2; " +
                "echo tcp_port=$(getprop service.adb.tcp.port); " +
                "echo adbd=$(getprop init.svc.adbd)";
        return runNormal(command);
    }

    static String getProperty(String name) {
        ShellResult result = runNormal("getprop " + safePropertyName(name));
        return result.output;
    }

    static ShellResult runNormal(String command) {
        return run(new String[]{"sh", "-c", command});
    }

    private static ShellResult runRoot(String command) {
        return run(new String[]{"su", "-c", command});
    }

    private static ShellResult run(String[] command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            boolean finished = false;
            int exitCode = -1;
            while (System.currentTimeMillis() < deadline) {
                try {
                    exitCode = process.exitValue();
                    finished = true;
                    break;
                } catch (IllegalThreadStateException ignored) {
                    Thread.sleep(100L);
                }
            }

            if (!finished) {
                process.destroy();
                return new ShellResult(-1, "انتهت مهلة الأمر", true);
            }

            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
            reader.close();
            return new ShellResult(exitCode, output.toString(), false);
        } catch (Exception error) {
            return new ShellResult(-1, error.getClass().getSimpleName() + ": " + error.getMessage(), false);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String safePropertyName(String name) {
        if (name != null && name.matches("[a-zA-Z0-9._-]+")) {
            return name;
        }
        return "invalid.property";
    }
}
