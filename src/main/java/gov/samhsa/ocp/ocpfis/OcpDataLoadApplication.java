package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.organization.TempOrganizationDto;
import gov.samhsa.ocp.ocpfis.model.patientlist.WrapperPatientDto;
import gov.samhsa.ocp.ocpfis.model.practitioner.Code;
import gov.samhsa.ocp.ocpfis.model.practitioner.Name;
import gov.samhsa.ocp.ocpfis.model.practitioner.PractitionerRole;
import gov.samhsa.ocp.ocpfis.model.practitioner.WrapperPractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class OcpDataLoadApplication {

    public static void main(String[] args) throws IOException, InvalidFormatException, JSONException {
        readPropertiesFile();
        log.info("Read properties file");

        //setUAAUrlInScripts(DataConstants.uaaUrl);
        //log.info("Finished running dos2unix and sed commands for shell scripts");

        if(DataConstants.runFhirOnly && !DataConstants.runUAAOnly) {
            populateFhirResources();
            log.info("Populated fhir resources");

        }

        if(!DataConstants.runFhirOnly && DataConstants.runUAAOnly) {
            populateUAA();
            log.info("Populated UAA resources");
        }

        if(DataConstants.runFhirOnly && DataConstants.runUAAOnly) {
            populateFhirResources();
            log.info("Populated fhir resources");

            populateUAA();
            log.info("Populated UAA resources");
        }

        log.info("Completed the job!!!");
    }

    private static void setUAAUrlInScripts(String uaaUrl) {
        //TODO: Not working. Not used in the application yet.
        try {
            //change file format to unix
            log.info("Running dos2unix *.sh");
            execCommand("dos2unix external-scripts/*.sh");

            //file 1
            log.info("Running sed for ./create-user-with-attributes.sh");
            execCommand("sed -i 's*http://localhost:8080/*" + uaaUrl + "*g' external-scripts/create-user-with-attributes.sh");

            //file 2
            log.info("Running sed for ./create-role-with-scope.sh");
            execCommand("sed -i 's*http://localhost:8080/*" + uaaUrl + "*g' external-scripts/create-role-with-scope.sh");

            //file 3
            log.info("Running sed for ./create-smart-admin-role-with-scope.sh");
            execCommand("sed -i 's*http://localhost:8080/*" + uaaUrl + "*g' external-scripts/create-smart-admin-role-with-scope.sh");

            //file 4
            log.info("Running sed for ./create-smart-role-with-scope.sh");
            execCommand("sed -i 's*http://localhost:8080/*" + uaaUrl + "*g' external-scripts/create-smart-role-with-scope.sh");


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void execCommand(String command) throws InterruptedException, IOException {
        log.info("Command : " + command);
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        System.out.println("exit: " + p.exitValue());
        p.destroy();
    }

    private static void readPropertiesFile() {
        Properties prop = new Properties();
        String[] values = new String[4];
        try (InputStream input = new FileInputStream("data.properties")) {
            prop.load(input);

            DataConstants.xlsxFile = prop.getProperty("xlsxfile");
            DataConstants.valueSetDir = prop.getProperty("valuesetsdir");
            DataConstants.scriptsDir = prop.getProperty("scriptsdir");
            DataConstants.serverUrl = prop.getProperty("fisUrl");
            DataConstants.runFhirOnly = Boolean.parseBoolean(prop.getProperty("runFhirOnly"));
            DataConstants.runUAAOnly = Boolean.parseBoolean(prop.getProperty("runUAAOnly"));
            DataConstants.uaaUrl = prop.getProperty("uaaUrl");
            DataConstants.fhirUrl = prop.getProperty("fhirUrl");
            DataConstants.structureDefDir = prop.getProperty("structureDefDir");

            log.info("xlsx file :" + DataConstants.xlsxFile);
            log.info("valuesets location : " + DataConstants.valueSetDir);
            log.info("structureDefDir dir : " + DataConstants.structureDefDir);
            log.info("scripts location : " + DataConstants.scriptsDir);
            log.info("fis server : " + DataConstants.serverUrl);
            log.info("UAA server : " + DataConstants.uaaUrl);
            log.info("Fhir server : " + DataConstants.fhirUrl);
            log.info("run fhir only : " + DataConstants.runFhirOnly);
            log.info("run UAA only : " + DataConstants.runUAAOnly);

        } catch (IOException e) {
            log.error("Please provide a file data.properties at the root directory");
        }

    }

    private static void populateUAA() throws IOException, InvalidFormatException {
        //1. Create roles and scopes
        log.info("Start creating roles an scopes in UAA");
        RolesUAAHelper.createRoles();
        log.info("Finished creating roles and scopes in UAA");
        log.info("------------------------------------------");

        //2. Create ocpAdmin
        log.info("Starting creating ocpAdmin in UAA");
        OCPAdminUAAHelper.createOCPAdmin();
        log.info("Finished creating ocpAdmin in UAA");
        log.info("------------------------------------------");

        Workbook workbook = WorkbookFactory.create(new File(DataConstants.xlsxFile));

        //Get all organizations
        log.info("Retrieving all organizations");
        Map<String, String> organizationsMap = retrieveOrganizations();
        log.info("Retrieved organizations");
        log.info("------------------------------------------");

        //3. Populate Practitioners
        log.info("Retrieving all practitioners");
        Map<String, String> practitionersMap = retrievePractitioners();
        List<PractitionerDto> practitionersSheet = PractitionersHelper.retrieveSheet(workbook.getSheet("Practitioners"), organizationsMap);
        log.info("Retrieved practitioners");
        log.info("------------------------------------------");


        log.info("Start creating practitioners in UAA");
        PractitionerUAAHelper.createPractitioners(practitionersMap, practitionersSheet);
        log.info("Finished creating practitioners in UAA");
        log.info("------------------------------------------");


        //4. Populate Patients
        log.info("Retrieving all patients");
        Map<String, String> patientsMap = retrievePatients();
        log.info("Retrieved patients");

        List<PatientDto> patientsSheet = PatientsHelper.retrieveSheet(workbook.getSheet("Patient"), practitionersMap, organizationsMap);

        log.info("Start creating patients in UAA");
        PatientUAAHelper.createPatients(patientsMap, organizationsMap, patientsSheet);
        log.info("Finished creating patients in UAA");
        log.info("------------------------------------------");

        //5. Populate Smart on Fhir roles
        log.info("Start SmartOnFhir roles");
        SmartOnFhirUAAHelper.runScript();

    }

    private static void populateFhirResources() throws IOException, InvalidFormatException, JSONException {

        //for intercepting the requests and debugging
        setFiddler();

        //ValueSets
        ValueSetHelper.process();

        //StructureDefinitions
        StructureDefinitionHelper.process();

        //Create a workbook form excel file
        Workbook workbook = WorkbookFactory.create(new File(DataConstants.xlsxFile));

        //Check number of sheets
        log.info("Number of sheets : " + workbook.getNumberOfSheets());

        //Organizations
        Sheet organizations = workbook.getSheet("Organizations");
        OrganizationHelper.process(organizations);

        //Get all organizations
        Map<String, String> mapOrganizations = retrieveOrganizations();
        log.info("Retrieved organizations");

        Sheet locations = workbook.getSheet("Locations");
        LocationsHelper.process(locations, mapOrganizations);
        log.info("Populated locations");

        Sheet healthCareServices = workbook.getSheet("Healthcare Services");
        HealthCareServicesHelper.process(healthCareServices, mapOrganizations);
        log.info("Populated healthCareServices");

        Sheet activityDefinitions = workbook.getSheet("Activity Definitions");
        //Do not enable
        ActivityDefinitionsHelper.process(activityDefinitions, mapOrganizations);
        log.info("Populated practitioners");

        Sheet practitioners = workbook.getSheet("Practitioners");
        PractitionersHelper.process(practitioners, mapOrganizations);
        log.info("Populated practitioners");

        Map<String, String> mapOfPractitioners = retrievePractitioners();

        Sheet patients = workbook.getSheet("Patient");
        PatientsHelper.process(patients, mapOfPractitioners, mapOrganizations);
        log.info("Populated patients");

        Map<String, String> mapOfPatients = retrievePatients();

        Sheet relationPersons = workbook.getSheet("Patient Related Persons");
        RelatedPersonsHelper.process(relationPersons, mapOfPatients);
        log.info("Populated relationPersons");

        // Commenting the upload of below resources as they can be entered from system easily
/*        Sheet careTeams = workbook.getSheet("Patient Care Teams");
        CareTeamsHelper.process(careTeams, mapOfPractitioners, mapOfPatients, mapOrganizations);
        log.info("Populated careTeams");

        Sheet taskOwners = workbook.getSheet("Tasks");
        TasksHelper.process(taskOwners, mapOfPatients, mapOfPractitioners, mapOrganizations);
        log.info("Populated taskOwners");

        Sheet todos = workbook.getSheet("To Do");
        TodosHelper.process(todos, mapOfPatients, mapOrganizations);
        log.info("Populated todosHelper");

        Sheet communications = workbook.getSheet("Communication");
        CommunicationsHelper.process(communications, mapOfPatients, mapOfPractitioners);
        log.info("Populated communications");

        Sheet appointments = workbook.getSheet("Appointments");
        AppointmentsHelper.process(appointments, mapOfPatients, mapOfPractitioners);
        log.info("Populated appointments");*/

        workbook.close();
        log.info("Workbook closed");

    }

    private static Map<String, String> retrieveOrganizations() {
        String orgsUrl = DataConstants.serverUrl + "organizations/search?showAll=true";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<TempOrganizationDto> foo = rt.getForEntity(orgsUrl, TempOrganizationDto.class);

        TempOrganizationDto tempOrganizationDto = foo.getBody();

        List<gov.samhsa.ocp.ocpfis.model.organization.Element> elements = tempOrganizationDto.getElements();

        Map<String, String> mapOrganizations = new HashMap<>();
        for (gov.samhsa.ocp.ocpfis.model.organization.Element element : elements) {
            mapOrganizations.put(element.getName().trim(), element.getLogicalId());
        }

        return mapOrganizations;
    }

    private static Map<String, String> retrievePractitioners() {
        String url = DataConstants.serverUrl + "practitioners/search?showAll=true";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<WrapperPractitionerDto> practitioners = rt.getForEntity(url, WrapperPractitionerDto.class);

        WrapperPractitionerDto wrapperDto = practitioners.getBody();

        List<gov.samhsa.ocp.ocpfis.model.practitioner.Element> dtos = wrapperDto.getElements();

        log.info("Number of Practitioners retrieved : " + dtos.size());

        Map<String, String> practitionersMap = new HashMap<>();
        for (gov.samhsa.ocp.ocpfis.model.practitioner.Element practitionerDto : dtos) {

            Name name = new Name();
            if (practitionerDto.getName().stream().findFirst().isPresent()) {
                name = practitionerDto.getName().stream().findFirst().get();
            } else {
                name.setFirstName("Unknown");
                name.setLastName("Unknown");
            }

            List<PractitionerRole> practitionRoles = practitionerDto.getPractitionerRoles();

            PractitionerRole practitionerRole = new PractitionerRole();
            if (practitionRoles.stream().findFirst().isPresent()) {
                practitionerRole = practitionRoles.stream().findFirst().get();
            } else {
                Code code = new Code();
                code.setCode("214"); //Org Admin
                code.setDisplay("Organization Administrator");
                practitionerRole.setLogicalId("214");
                practitionerRole.setCode(Arrays.asList(code));

            }

            log.info("practitionerRole : " + practitionerRole.getLogicalId() + " Value : " + practitionerRole.getCode().stream().findFirst().get().getDisplay());

            practitionersMap.put(name.getFirstName().trim() + " " + name.getLastName().trim(), practitionerDto.getLogicalId());
        }

        return practitionersMap;
    }

    private static Map<String, String> retrievePatients() {
        String url = DataConstants.serverUrl + "patients/search?showAll=true";
        RestTemplate rt = new RestTemplate();
        ResponseEntity<WrapperPatientDto> responseEntity = rt.getForEntity(url, WrapperPatientDto.class);
        WrapperPatientDto wrapperDto = responseEntity.getBody();

        List<gov.samhsa.ocp.ocpfis.model.patientlist.Element> dtos = wrapperDto.getElements();
        Map<String, String> patientsMap = new HashMap<>();

        for (gov.samhsa.ocp.ocpfis.model.patientlist.Element patientDto : dtos) {
            gov.samhsa.ocp.ocpfis.model.patientlist.Name name = new gov.samhsa.ocp.ocpfis.model.patientlist.Name();
            if (patientDto.getName().stream().findFirst().isPresent()) {
                name = patientDto.getName().stream().findFirst().get();
            } else {
                name.setFirstName("Unknown");
                name.setLastName("Unknown");
                name.setUserName("Unknown");
            }

            patientsMap.put(name.getFirstName().trim() + " " + name.getLastName().trim(), patientDto.getId());
        }

        return patientsMap;
    }

    private static void setFiddler() {
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyPort", "8888");
    }


}
