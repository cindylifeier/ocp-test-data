package gov.samhsa.ocp.ocpfis.model.patientlist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Element {

    private String id;
    private String resourceURL;
    private List<Identifier> identifier = null;
    private Boolean active;
    private List<Name> name = null;
    private String genderCode;
    private String birthDate;
    private String ethnicity;
    private String birthSex;
    private List<Address> addresses = null;
    private List<Telecom> telecoms = null;
    private String language;
    private List<Object> flags = null;
    private String race;
    private String userName;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

}
