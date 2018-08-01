package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.relatedperson.TempRelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointSystem;
import org.hl7.fhir.dstu3.model.codesystems.ContactPointUse;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RelatedPersonsHelper {

    public static void process(Sheet relatedPersons, Map<String, String> mapOfPatients) {
        log.info("Last row number :" + relatedPersons.getLastRowNum());
        int rowNum = 0;

        List<TempRelatedPersonDto> relatedPersonDtos = retrieveSheet(relatedPersons, mapOfPatients, rowNum);

        RestTemplate rt = new RestTemplate();

        log.info("Size : " + relatedPersonDtos.size());


        relatedPersonDtos.forEach(relatedPersonDto -> {
            log.info("related persons : " + relatedPersonDto);

            try {
                if (relatedPersonDto.getPatient() != null) {
                    HttpEntity<TempRelatedPersonDto> request = new HttpEntity<>(relatedPersonDto);
                    rt.postForObject(DataConstants.serverUrl + "related-persons/", request, RelatedPersonDto.class);
                }
            } catch (Exception e) {
                log.info("This relatedPerson could not be posted : " + relatedPersonDto);
                e.printStackTrace();

            }

        });

    }

    private static List<TempRelatedPersonDto> retrieveSheet(Sheet relatedPersons, Map<String, String> mapOfPatients, int rowNum) {
        List<TempRelatedPersonDto> relatedPersonDtos = new ArrayList<>();
        Map<String, String> identifierTypeLookup = CommonHelper.identifierTypeDtoValue(DataConstants.serverUrl + "lookups/identifier-systems");
        Map<String, String> genderLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/administrative-genders");
        Map<String, String> relationLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/related-person-patient-relationship-types");
        for (Row row : relatedPersons) {
            if (rowNum > 0) {
                int j = 0;
                TempRelatedPersonDto dto = new TempRelatedPersonDto();

                dto.setStartDate("01/01/2018");
                dto.setEndDate("01/01/2020");
                try {
                    processRow(mapOfPatients, identifierTypeLookup, genderLookup, relationLookup, row, j, dto);
                    relatedPersonDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing row of a relatedPerson");
                }

            }
            rowNum++;
        }
        return relatedPersonDtos;
    }

    private static void processRow(Map<String, String> mapOfPatients, Map<String, String> identifierTypeLookup, Map<String, String> genderLookup, Map<String, String> relationLookup, Row row, int j, TempRelatedPersonDto dto) {
        String line = "";
        String city="";
        String state="";
        String zip="";
        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell).trim();

            if (j == 0) {
                dto.setPatient(mapOfPatients.get(cellValue.trim()));
            } else if (j == 1) {
                dto.setFirstName(cellValue);
            } else if (j == 2) {
                dto.setLastName(cellValue);
            } else if (j == 3) {
                dto.setRelationshipCode(relationLookup.get(cellValue));
                dto.setRelationshipValue(cellValue);
            } else if (j == 4) {
                dto.setBirthDate("01/01/1980");
            } else if (j == 5) {
                dto.setGenderCode(genderLookup.get(cellValue));
                dto.setGenderValue(cellValue);
            } else if (j == 6) {
                dto.setIdentifierType(identifierTypeLookup.get(cellValue));
            } else if (j == 7) {
                dto.setIdentifierValue(cellValue);
            } else if (j == 8) {
                boolean isActive = cellValue.equalsIgnoreCase("active");
                dto.setActive(isActive);
            } else if(j==9){
                line=cellValue.trim();
            }else if(j==10){
                city=cellValue.trim();
            }else if(j==11){
                state=cellValue.trim();
            } else if (j == 12) {
                zip=cellValue.trim();
                dto.setAddresses(CommonHelper.getAddresses(line,city,state,zip));
            } else if (j == 13) {
                TelecomDto telecomDto = new TelecomDto();
                telecomDto.setSystem(java.util.Optional.of(ContactPointSystem.PHONE.toCode()));
                telecomDto.setUse(java.util.Optional.of(ContactPointUse.WORK.toCode()));
                telecomDto.setValue(java.util.Optional.of(cellValue));
                dto.setTelecoms(Collections.singletonList(telecomDto));
            }
            j++;
        }
    }
}
