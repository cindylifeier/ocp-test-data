package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointSystem;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointUse;
import org.hl7.fhir.dstu3.model.codesystems.EpisodeofcareType;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PatientsHelper {

    public static void process(Sheet patients, Map<String, String> mapOfPractitioners, Map<String, String> mapOfOrganizations) {

        List<PatientDto> patientDtos = retrieveSheet(patients, mapOfPractitioners, mapOfOrganizations);

        RestTemplate rt = new RestTemplate();

        patientDtos.forEach(patientDto -> {
            try {
                HttpEntity<PatientDto> request = new HttpEntity<>(patientDto);
                rt.postForObject(DataConstants.serverUrl + "patients/", request, PatientDto.class);
            } catch (Exception e) {
                log.info("This patient could not be posted : " + patientDto);
                e.printStackTrace();
            }
        });
    }

    public static List<PatientDto> retrieveSheet(Sheet patients, Map<String, String> mapOfPractitioners, Map<String, String> mapOfOrganizations) {
        Map<String, String> genderCodeLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/administrative-genders");
        Map<String, String> birthSexLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-birthsexes");
        Map<String, String> raceLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-races");
        Map<String, String> ethnicityLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/us-core-ethnicities");
        Map<String, String> languageLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/languages");
        Map<String, String> identifierTypeLookup = CommonHelper.identifierTypeDtoValue(DataConstants.serverUrl + "lookups/identifier-systems");

        List<PatientDto> patientDtos = new ArrayList<>();
        int rowNum = 0;
        for (Row row : patients) {
            if (rowNum > 0) {
                int j = 0;
                PatientDto dto = new PatientDto();
                try {
                    processRow(mapOfPractitioners, mapOfOrganizations, genderCodeLookup, birthSexLookup, raceLookup, ethnicityLookup, languageLookup, identifierTypeLookup, row, j, dto);
                    // Add default Episode Of Care
                    createDefaultEpisodeOfCareDto(dto);
                    patientDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing a row of patient");
                    e.printStackTrace();
                }

            }
            rowNum++;
        }
        return patientDtos;
    }

    private static void processRow(Map<String, String> mapOfPractitioners, Map<String, String> mapOfOrganizations, Map<String, String> genderCodeLookup, Map<String, String> birthSexLookup, Map<String, String> raceLookup, Map<String, String>
            ethnicityLookup, Map<String, String> languageLookup, Map<String, String> identifierTypeLookup, Row row, int j, PatientDto dto) {
        NameDto nameDto = new NameDto();
        IdentifierDto tempIdentifierDto = new IdentifierDto();

        List<TelecomDto> telecomDtos = new ArrayList<>();

        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell);

            if (j == 0) {
                nameDto.setFirstName(cellValue);
            } else if (j == 1) {
                nameDto.setLastName(cellValue);
            } else if (j == 2) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                    LocalDate localDate = LocalDate.parse(cellValue, formatter);
                    dto.setBirthDate(localDate);
                } catch (Exception e) {
                    //use some default
                    dto.setBirthDate(LocalDate.of(1986, Month.APRIL, 1));
                }
            } else if (j == 3) {
                dto.setGenderCode(genderCodeLookup.get(cellValue));
            } else if (j == 4) {
                dto.setBirthSex(birthSexLookup.get(cellValue));
            } else if (j == 5) {
                dto.setRace(raceLookup.get(cellValue));
            } else if (j == 6) {
                dto.setEthnicity(ethnicityLookup.get(cellValue)); // TODO: This is not being displayed when edited from UI
            } else if (j == 7) {
                dto.setLanguage(languageLookup.get(cellValue));
            } else if (j == 8) {
                if (cellValue!= null && !cellValue.trim().isEmpty() && cellValue.trim().equalsIgnoreCase(ConstantsUtil.SSN_DISPLAY)) {
                    tempIdentifierDto.setSystem(ConstantsUtil.SSN_URI);
                    tempIdentifierDto.setOid(ConstantsUtil.SSN_URI);
                    tempIdentifierDto.setDisplay(ConstantsUtil.SSN_DISPLAY);
                } else if (cellValue!= null && !cellValue.trim().isEmpty() && cellValue.trim().equalsIgnoreCase(ConstantsUtil.MEDICARE_NUMBER_DISPLAY)) {
                    tempIdentifierDto.setSystem(ConstantsUtil.MEDICARE_NUMBER_URI);
                    tempIdentifierDto.setOid(ConstantsUtil.MEDICARE_NUMBER_URI);
                    tempIdentifierDto.setDisplay(ConstantsUtil.MEDICARE_NUMBER_DISPLAY);
                } else if (cellValue!= null && !cellValue.trim().isEmpty() && cellValue.trim().equalsIgnoreCase(ConstantsUtil.IND_TAX_ID_DISPLAY)) {
                    tempIdentifierDto.setSystem(ConstantsUtil.IND_TAX_ID_URI);
                    tempIdentifierDto.setOid(ConstantsUtil.IND_TAX_ID_URI);
                    tempIdentifierDto.setDisplay(ConstantsUtil.IND_TAX_ID_DISPLAY);
                }
            } else if (j == 9) {
                tempIdentifierDto.setValue(cellValue);
                dto.setIdentifier(Collections.singletonList(tempIdentifierDto));
            } else if (j == 10) {
                TelecomDto telecomDto = new TelecomDto();
                telecomDto.setSystem(Optional.of(ContactPointSystem.PHONE.toCode()));
                telecomDto.setUse(Optional.of(ContactPointUse.WORK.toCode()));
                telecomDto.setValue(Optional.ofNullable(cellValue));
                telecomDtos.add(telecomDto);

            } else if (j == 11) {
                dto.setAddresses(CommonHelper.getAddresses(cellValue));
            } else if (j == 12) {
                // Advisory - Do nothing
            } else if (j == 13) {
                dto.setOrganizationId(Optional.of(mapOfOrganizations.get(cellValue.trim())));

            } else if (j == 14) {
                dto.setPractitionerId(Optional.of(mapOfPractitioners.get(cellValue.trim())));
            } else if (j == 15) {
                nameDto.setUserName(cellValue);
                dto.setName(Collections.singletonList(nameDto));
            } else if (j == 16) {

                TelecomDto emailDto = new TelecomDto();
                emailDto.setSystem(Optional.of(ContactPointSystem.EMAIL.toCode()));
                emailDto.setUse(Optional.of(ContactPointUse.WORK.toCode()));
                emailDto.setValue(Optional.ofNullable(cellValue));
                telecomDtos.add(emailDto);
                dto.setTelecoms(telecomDtos);
            }
            j++;

        }
    }

    private static void createDefaultEpisodeOfCareDto(PatientDto patientDto) {
        EpisodeOfCareDto episodeOfCareDto = new EpisodeOfCareDto();
        episodeOfCareDto.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE.toCode());
        episodeOfCareDto.setType(EpisodeofcareType.HACC.toCode());
        episodeOfCareDto.setStartDate(DateUtil.convertLocalDateToString(LocalDate.now()));
        episodeOfCareDto.setEndDate(DateUtil.convertLocalDateToString(LocalDate.now().plusYears(ConstantsUtil.EPISODE_OF_CARE_END_PERIOD)));
        patientDto.setEpisodeOfCares(Collections.singletonList(episodeOfCareDto));
    }
}
