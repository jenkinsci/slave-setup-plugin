package org.jenkinsci.plugins.slave_setup;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Shell;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {
    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        SetupConfig config = SetupConfig.get();

        File sd = config.getFilesDir();
        if (sd!=null) {
            listener.getLogger().println("Copying setup script files");
            new FilePath(sd).copyRecursiveTo(root);
        }

        String cmdLine = config.getCommandLine();
        if (cmdLine!=null) {
            listener.getLogger().println("Executing setup script");
            Node node = c.getNode();
            Launcher launcher = root.createLauncher(listener);
            Shell s = new Shell(cmdLine);
            FilePath script = s.createScriptFile(root);
            int r = launcher.launch().cmds(s.buildCommandLine(script)).envs(getEnvironment(node)).stdout(listener).pwd(root).join();

            if (r!=0)
                throw new AbortException("Setup script failed");

            listener.getLogger().println("Setup script completed successfully");
        }
    }

    private EnvVars getEnvironment(Node node) {
        EnvironmentVariablesNodeProperty env = node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        return env != null ? env.getEnvVars() : new EnvVars();
    }
}