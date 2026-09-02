package com.abosultan.darbaksystem.bridge;

final class ShellResult {
    final int exitCode;
    final String output;
    final boolean timedOut;

    ShellResult(int exitCode, String output, boolean timedOut) {
        this.exitCode = exitCode;
        this.output = output == null ? "" : output.trim();
        this.timedOut = timedOut;
    }

    boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }
}
