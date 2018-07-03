package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.healthcareservice.TempHealthCareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class HealthCareServicesHelper {

    public static void process(Sheet healthCareServices, Map<String, String> mapOrganizations) {
        System.out.println("Last row number : " + healthCareServices.getLastRowNum());

        int rowNum = 0;

        Map<String, String> categoryLookups = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/healthcare-service-categories");
        Map<String, String> typeLookups = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/healthcare-service-types");
        Map<String, String> specialityLookups = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/healthcare-service-specialities");
        Map<String, String> referralLookups = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/healthcare-service-referral-methods");

        List<TempHealthCareServiceDto> healthCareServiceDtos = retrieveSheet(healthCareServices, mapOrganizations, rowNum, categoryLookups, typeLookups, specialityLookups, referralLookups);

        RestTemplate rt = new RestTemplate();

        healthCareServiceDtos.forEach(healthcareServiceDto -> {
            try {
                log.info("healthcareServiceDto : " + healthcareServiceDto);
                HttpEntity<TempHealthCareServiceDto> request = new HttpEntity<>(healthcareServiceDto);
                rt.postForObject(DataConstants.serverUrl + "organization/" + healthcareServiceDto.getOrganizationId() + "/healthcare-services", request, TempHealthCareServiceDto.class);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }

    private static List<TempHealthCareServiceDto> retrieveSheet(Sheet healthCareServices, Map<String, String> mapOrganizations, int rowNum, Map<String, String> categoryLookups, Map<String, String> typeLookups, Map<String, String> specialityLookups, Map<String, String> referralLookups) {
        List<TempHealthCareServiceDto> healthCareServiceDtos = new ArrayList<>();

        for (Row row : healthCareServices) {
            if (rowNum > 0) {
                int j = 0;
                TempHealthCareServiceDto dto = new TempHealthCareServiceDto();
                try {
                    processRow(mapOrganizations, categoryLookups, typeLookups, specialityLookups, referralLookups, row, j, dto);
                    healthCareServiceDtos.add(dto);

                } catch (Exception e) {
                    log.error("Error processing a row for healthCareServices");
                }

            }
            rowNum++;
        }
        return healthCareServiceDtos;
    }

    private static void processRow(Map<String, String> mapOrganizations, Map<String, String> categoryLookups, Map<String, String> typeLookups, Map<String, String> specialityLookups, Map<String, String> referralLookups, Row row, int j, TempHealthCareServiceDto dto) {
        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell);

            if (j == 0) {
                //organization
                dto.setOrganizationId(mapOrganizations.get(cellValue.trim()));
            } else if (j == 1) {
                //name
                dto.setName(cellValue);
            } else if (j == 2) {
                //program name
                dto.setProgramName(Arrays.asList(cellValue));
            } else if (j == 3) {
                //category

                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(categoryLookups.get(cellValue.trim()));

                if (valueSetDto.getCode() == null) {
                    valueSetDto.setCode(categoryLookups.get("Adoption"));
                }

                dto.setCategory(valueSetDto);
            } else if (j == 4) {
                //type

                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(typeLookups.get(cellValue.trim()));
                valueSetDto.setSystem("http://hl7.org/fhir/service-type");

                if (valueSetDto.getCode() == null) {
                    valueSetDto.setCode(typeLookups.get("Aged Care Assessment"));
                }

                dto.setType(Arrays.asList(valueSetDto));
            } else if (j == 5) {
                //speciality

                ValueSetDto valueSetDto = new ValueSetDto();

                //if not available, set a default value
                if (valueSetDto.getCode() == null) {
                    valueSetDto.setCode(specialityLookups.get("Adult mental illness"));
                }

                dto.setSpecialty(Arrays.asList(valueSetDto));
            } else if (j == 6) {
                //referral

                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(referralLookups.get(cellValue.trim()));

                if (valueSetDto.getCode() == null) {
                    valueSetDto.setCode(referralLookups.get("Phone"));
                }

                dto.setReferralMethod(Arrays.asList(valueSetDto));
            } else if (j == 7) {
                //contact
                log.info(cellValue);
                TelecomDto telecomDto = new TelecomDto();
                telecomDto.setValue(Optional.of(cellValue));
                dto.setTelecom(Arrays.asList(telecomDto));
            }

            j++;
        }
    }

}
