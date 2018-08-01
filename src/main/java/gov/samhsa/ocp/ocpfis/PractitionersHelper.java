package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PractitionersHelper {

    public static void process(Sheet practitioners, Map<String, String> mapOrganizations) {
        List<PractitionerDto> practitionerDtos = retrieveSheet(practitioners, mapOrganizations);

        RestTemplate rt = new RestTemplate();
        practitionerDtos.forEach(practitionerDto -> {
            try {
                HttpEntity<PractitionerDto> request = new HttpEntity<>(practitionerDto);

                log.info("Getting ready to post: " + practitionerDto);
                List<IdentifierDto> identifierDtos = practitionerDto.getIdentifiers();

                Optional<IdentifierDto> oIdentifierDto = identifierDtos.stream().findFirst();
                if (oIdentifierDto.isPresent()) {
                    IdentifierDto identifierDto = oIdentifierDto.get();

                    if (identifierDto.getValue() != null) {
                        rt.postForObject(DataConstants.serverUrl + "practitioners", request, PractitionerDto.class);
                    }
                }
            } catch (Exception e) {
                log.error("Practitioner could not be posted");
            }
        });

    }

    public static List<PractitionerDto> retrieveSheet(Sheet practitioners, Map<String, String> mapOrganizations) {
        log.info("last row number : " + practitioners.getLastRowNum());

        int rowNum = 0;

        Map<String, String> practitionerRolesLookup = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/provider-role");
        List<PractitionerDto> practitionerDtos = new ArrayList<>();

        for (Row row : practitioners) {
            if (rowNum > 0) {
                int j = 0;
                PractitionerDto dto = new PractitionerDto();
                try {
                    processRow(mapOrganizations, practitionerRolesLookup, row, j, dto);
                    practitionerDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing row of a practitioner");
                }

            }
            rowNum++;
        }

        return practitionerDtos;
    }

    private static void processRow(Map<String, String> mapOrganizations, Map<String, String> practitionerRolesLookup, Row row, int j, PractitionerDto dto) {
        String firstName = "";
        String line = "";
        String city = "";
        String state = "";
        String zip;
        PractitionerRoleDto roleDto = new PractitionerRoleDto();
        IdentifierDto identifierDto = new IdentifierDto();
        roleDto.setActive(true);

        ValueSetDto specialtyValueSetDto = new ValueSetDto();
        specialtyValueSetDto.setCode(ConstantsUtil.PRACTITIONER_DEFAULT_SPECIALITY_CODE);
        specialtyValueSetDto.setDisplay(ConstantsUtil.PRACTITIONER_DEFAULT_SPECIALITY_DISPLAY);
        specialtyValueSetDto.setSystem(ConstantsUtil.PRACTITIONER_SPECIALITY_SYSTEM);
        roleDto.setSpecialty(Collections.singletonList(specialtyValueSetDto));

        List<TelecomDto> telecoms = new ArrayList<>();
        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell);

            if (j == 0) {
                //organization
                ReferenceDto referenceDto = new ReferenceDto();
                referenceDto.setDisplay(cellValue);
                referenceDto.setReference(ResourceType.Organization + "/" + mapOrganizations.get(cellValue.trim()));
                roleDto.setOrganization(referenceDto);

                dto.setPractitionerRoles(Collections.singletonList(roleDto));

            } else if (j == 1) {
                //first name

                firstName = cellValue.trim();

            } else if (j == 3) {
                //last name
                NameDto nameDto = new NameDto();
                nameDto.setFirstName(firstName);
                nameDto.setLastName(cellValue);

                dto.setName(Collections.singletonList(nameDto));

            } else if (j == 4) {
                //role

                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(practitionerRolesLookup.get(cellValue.trim()));
                valueSetDto.setDisplay(cellValue);

                //put a default if not available
                if (valueSetDto.getCode() == null) {
                    valueSetDto.setCode(practitionerRolesLookup.get(ConstantsUtil.PRACTITIONER_DEFAULT_ROLE_DISPLAY));
                }
                valueSetDto.setSystem(ConstantsUtil.PRACTITIONER_ROLE_SYSTEM);
                roleDto.setCode(Collections.singletonList(valueSetDto));

            } else if (j == 5) {
                //address
                line = cellValue.trim();

            } else if (j == 6) {
                city = cellValue.trim();

            } else if (j == 7) {
                state = cellValue.trim();
            } else if (j == 8) {
                zip = cellValue.trim();
                dto.setAddresses(CommonHelper.getAddresses(line, city, state, zip));
            } else if (j == 9) {
                //contact
                TelecomDto telecom = new TelecomDto();
                telecom.setSystem(Optional.of(ConstantsUtil.PHONE_SYSTEM));
                telecom.setValue(Optional.of(cellValue));
                telecoms.add(telecom);

            } else if (j == 10) {
                //email
                TelecomDto telecom = new TelecomDto();
                telecom.setSystem(Optional.of(ConstantsUtil.EMAIL_SYSTEM));
                telecom.setValue(Optional.of(cellValue));
                telecoms.add(telecom);

                dto.setTelecoms(telecoms);

            } else if (j == 11) {
                // ID System
                if (cellValue!= null && !cellValue.trim().isEmpty() && cellValue.trim().equalsIgnoreCase(ConstantsUtil.NPI_DISPLAY)) {
                    identifierDto.setSystem(ConstantsUtil.NPI_URI);
                    identifierDto.setDisplay(ConstantsUtil.NPI_DISPLAY);
                }

            } else if (j == 12) {
                // ID Value
                identifierDto.setValue(cellValue);
                dto.setIdentifiers(Collections.singletonList(identifierDto));

            } else if (j == 13) {
                //UAA role
                dto.setUaaRole(cellValue);
            } else if (j == 14) {

                dto.setUsername(cellValue);
            }

            j++;
        }
    }

}
