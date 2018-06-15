package gov.samhsa.ocp.ocpfis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class UAADataLoadApplication {

    public static void main(String[] args) {
        try {
            //ProcessBuilder pb = new ProcessBuilder("/bin/bash", "leaptest.sh");
            ProcessBuilder pb = new ProcessBuilder("c:\\data\\leaptest.sh");
            final Process process = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            PrintWriter pw = new PrintWriter(process.getOutputStream());
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                pw.println("1997");
                pw.flush();
            }
            System.out.println("Program terminated!");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
