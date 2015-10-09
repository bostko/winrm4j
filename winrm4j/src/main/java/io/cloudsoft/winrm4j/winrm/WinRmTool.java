package io.cloudsoft.winrm4j.winrm;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClient.Builder;

/**
 * Tool for executing commands over WinRM.
 */
public class WinRmTool {
    private String address;
    private String username;
    private String password;

    public static WinRmTool connect(String address, String username, String password) {
        return new WinRmTool(address, username, password);
    }

    private WinRmTool(String address, String username, String password) {
        this.address = address;
        this.username = username;
        this.password = password;
    }

    /**
     * Execute a list of Windows Native commands as one command.
     * The method translates the list of commands to a single String command with a <code>"\r\n"</code> delimiter and a terminating one.
     * @param commands
     * @deprecated since 0.2; Use the {@link #executeCommand(String)} instead and transform your commands list explicitly
     */
    @Deprecated
    public WinRmToolResponse executeScript(List<String> commands) {
        return executeCommand(joinScript(commands));
    }

    /**
     * Executes a Native Windows command.
     * It is creating a new Shell on the destination host each time it is being called.
     * @param command
     * @since 0.2
     */
    public WinRmToolResponse executeCommand(String command) {
        Builder builder = WinRmClient.builder(getEndpointUrl());
        if (username != null && password != null) {
            builder.credentials(username, password);
        }
        WinRmClient client = builder.build();

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        try {
            int code = client.command(command, out, err);
            return new WinRmToolResponse(out.toString(), err.toString(), code);
        } finally {
            client.disconnect();
        }
    }

    //TODO support https transport
    private String getEndpointUrl() {
        if (address.startsWith("http:") || address.startsWith("https:")) {
            return address;
        } else if (address.contains(":")) {
            return "http://" + address + "/wsman";
        } else {
            return "http://" + address + ":5985/wsman";
        }
    }

    /**
     * Executes a Power Shell command.
     * It is creating a new Shell on the destination host each time it is being called.
     * @param psCommand
     * @since 0.2
     */
    public WinRmToolResponse executePsCommand(String psCommand) {
        return executeCommand(compilePs(psCommand));
    }

    /**
     * Execute a list of Power Shell commands as one command.
     * The method translates the list of commands to a single String command with a <code>"\r\n"</code> delimiter and a terminating one.
     * @param commands
     * @deprecated since 0.2; Use the {@link #executePsCommand(String)} instead and transform your commands list explicitly
     */
    @Deprecated
    public WinRmToolResponse executePsScript(List<String> commands) {
        return executeCommand(compilePs(joinPs(commands)));
    }

    private String compilePs(String psScript) {
        byte[] cmd = psScript.getBytes(Charset.forName("UTF-16LE"));
        String arg = javax.xml.bind.DatatypeConverter.printBase64Binary(cmd);
        return "powershell -encodedcommand " + arg;
    }

    /**
     * Batch Script commands appear verbatim in the XML and JAXB will
     * serialize "\r\n" as "&#xD;\n" in XML which is not recognized
     * by the Windows service (doesn't unescape &#xD;). Since new lines
     * in XML are represented as a single "\n" anyway it's fine to
     * use it as a separator here.
     * 
     * TODO cover the case where \r appears in the command?
     */
    private String joinScript(List<String> commands) {
        return join(commands, "\n");
    }

    /**
     * PS commands are base64 encoded so we can use the normal new line
     * Windows delimiter here.
     */
    private String joinPs(List<String> commands) {
        return join(commands, "\r\n");
    }

    private String join(List<String> commands, String delim) {
        StringBuilder builder = new StringBuilder();
        for (String command : commands) {
            builder.append(command)
                .append(delim);

        }
        return builder.toString();
    }
}
