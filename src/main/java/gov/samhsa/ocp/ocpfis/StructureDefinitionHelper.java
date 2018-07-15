package gov.samhsa.ocp.ocpfis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StructureDefinitionHelper {

    public static void process() throws JSONException, IOException {
        File[] files = listFiles(new File(DataConstants.structureDefDir));

        Map<String, String> map = new HashMap<>();

        for (File file : files) {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String str = new String(data, "UTF-8");
            map.put(file.getName().replace(".json", ""), str);
        }

        RestTemplate rt = new RestTemplate();

        map.forEach((key, value) -> {
            try {
                HttpHeaders headers = new HttpHeaders();

                MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<String, String>();
                multiValueMap.add("serverId", "home");
                multiValueMap.add("pretty", "true");
                multiValueMap.add("resource", "StructureDefinition");
                multiValueMap.add("resource-create-id", key);
                multiValueMap.add("resource-create-body", value);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(multiValueMap, headers);

                ResponseEntity<String> response = rt.postForEntity(DataConstants.fhirUrl + "fhir/update", request, String.class);

                log.info("Response status code : " + response.getStatusCode());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private static File[] listFiles(final File folder) {
        return folder.listFiles();
    }

}

