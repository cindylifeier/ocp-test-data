package gov.samhsa.ocp.ocpfis.model.patientlist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Name {

    private String firstName;
    private String lastName;
    private String userName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
}
