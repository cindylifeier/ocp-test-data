package gov.samhsa.ocp.ocpfis;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@Slf4j
public class SmartOnFhirUAAHelper {

    public static void runScript() {
        log.info("Populating create-smart-admin-role-with-scope.sh...");
        runScript("create-smart-admin-role-with-scope.sh");

        log.info("Populating create-smart-role-with-scope.sh");
        runScript("create-smart-role-with-scope.sh");
    }

    private static void runScript(String scriptName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", DataConstants.scriptsDir + "/" + scriptName);
            log.info("created processbuilder for " + scriptName);

            final Process process = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = br.readLine()) != null) {
                log.info(line);
            }

            log.info(scriptName + " terminated!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

