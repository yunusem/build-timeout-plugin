/*
 * The MIT License
 *
 * Copyright (c) 2019 Yunusemre Şentürk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.build_timeout.operations;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.build_timeout.BuildTimeOutOperation;
import hudson.plugins.build_timeout.BuildTimeOutOperationDescriptor;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.lang.ProcessBuilder;

import java.io.IOException;
import java.text.MessageFormat;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Execute shell script for the build timed out.
 */
public class ExecuteShellOperation extends BuildTimeOutOperation {
    private final String command;

    /**
     * @return command to execute.
     */
    public String getCommand() {
        return command;
    }

    @DataBoundConstructor
    public ExecuteShellOperation(String command) {
        this.command = command;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, BuildListener listener, long effectiveTimeout) {
        // timed out
        if (StringUtils.isEmpty(getCommand())) {
            listener.getLogger().println("No valid timeout shell script found. Skipping!");
            return true;
        }
        if (System.getProperty("os.name").startsWith("Win")) {
            listener.getLogger().println("Timeout execution shell are not supported on Windows");
            return false;
        }
        long effectiveTimeoutSeconds = SECONDS.convert(effectiveTimeout,MILLISECONDS);
        String tos = "";
        tos = MessageFormat.format(tos, effectiveTimeoutSeconds);
        listener.getLogger().println("Executing timeout shell script after " + tos + " seconds the build has started");
        BufferedReader br = null;
        File tempShell = null;
        ProcessBuilder pb;
        try {
            tempShell = File.createTempFile("jenkins", ".sh");
            FileWriter tmpShellWriter = new FileWriter(tempShell);
            pb = new ProcessBuilder("/bin/bash", tempShell.getAbsolutePath());
            tmpShellWriter.write(getCommand());
            tmpShellWriter.close();
            pb.redirectErrorStream(true);
            Process p = pb.start();

            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                listener.getLogger().println(line);
            }
            p.waitFor();
        } catch (IOException e) {
            listener.getLogger().println(
                    "Error while trying to execute timeout shell script: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            listener.getLogger().println(e.getMessage());
            return false;
        } finally {
            tempShell.delete();
            IOUtil.close(br);
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildTimeOutOperationDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ExecuteShellOperation_DisplayName();
        }
    }
}
