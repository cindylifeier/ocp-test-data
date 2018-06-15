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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PractitionersHelper {

    public static void process(Sheet practitioners, Map<String, String> mapOrganizations) {
        List<PractitionerDto> practitionerDtos = retrieveSheet(practitioners, mapOrganizations);

        RestTemplate rt = new RestTemplate();
        practitionerDtos.forEach(practitionerDto -> {
            HttpEntity<PractitionerDto> request = new HttpEntity<>(practitionerDto);

            log.info("Getting ready to post: " + practitionerDto);
            List<IdentifierDto> identifierDtos = practitionerDto.getIdentifiers();

            Optional<IdentifierDto> oIdentifierDto = identifierDtos.stream().findFirst();
            if(oIdentifierDto.isPresent()) {
                IdentifierDto identifierDto = oIdentifierDto.get();

                if (identifierDto.getValue() != null) {
                    rt.postForObject(DataConstants.serverUrl + "practitioners", request, PractitionerDto.class);
                }
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
                processRow(mapOrganizations, practitionerRolesLookup, row, j, dto);
                practitionerDtos.add(dto);
            }
            rowNum++;
        }

        return practitionerDtos;
    }

    private static void processRow(Map<String, String> mapOrganizations, Map<String, String> practitionerRolesLookup, Row row, int j, PractitionerDto dto) {
        String firstName = "";
        String middleName = "";
        PractitionerRoleDto roleDto = new PractitionerRoleDto();
        roleDto.setActive(true);

        ValueSetDto specialtyValueSetDto = new ValueSetDto();
        specialtyValueSetDto.setCode("103GC0700X");
        specialtyValueSetDto.setDisplay("Clinical");
        roleDto.setSpecialty(Arrays.asList(specialtyValueSetDto));

        List<TelecomDto> telecoms = new ArrayList<>();
        String idType = "";
        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell);

            if (j == 0) {
                //organization
                ReferenceDto referenceDto = new ReferenceDto();
                referenceDto.setDisplay(cellValue);
                referenceDto.setReference(ResourceType.Organization + "/" + mapOrganizations.get(cellValue.trim()));
                roleDto.setOrganization(referenceDto);

                dto.setPractitionerRoles(Arrays.asList(roleDto));

            } else if (j == 1) {
                //first name

                firstName = cellValue.trim();

            } else if (j == 2) {
                //middle name

                middleName = cellValue.trim();

            } else if (j == 3) {
                //last name
                NameDto nameDto = new NameDto();
                nameDto.setFirstName(firstName);
                nameDto.setLastName(cellValue);

                dto.setName(Arrays.asList(nameDto));

            } else if (j == 4) {
                //role

                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(practitionerRolesLookup.get(cellValue.trim()));
                valueSetDto.setDisplay(cellValue);

                //put a default if not available
                if(valueSetDto.getCode() == null) {
                    valueSetDto.setCode(practitionerRolesLookup.get("Counselor"));
                }

                roleDto.setCode(Arrays.asList(valueSetDto));

            } else if (j == 5) {
                //address

                dto.setAddresses(CommonHelper.getAddresses(cellValue));

            } else if (j == 6) {
                //contact
                TelecomDto telecom = new TelecomDto();
                telecom.setSystem(Optional.of("phone"));
                telecom.setValue(Optional.of(cellValue));
                telecoms.add(telecom);

            } else if (j == 7) {
                //email
                TelecomDto telecom = new TelecomDto();
                telecom.setSystem(Optional.of("email"));
                telecom.setValue(Optional.of(cellValue));
                telecoms.add(telecom);

                dto.setTelecoms(telecoms);

            } else if (j == 8) {
                //ID type

                idType = cellValue;

            } else if (j == 9) {
                //ID
                IdentifierDto identifierDto = new IdentifierDto();
                identifierDto.setSystem("http://hl7.org/fhir/sid/us-npi");
                identifierDto.setValue(cellValue);

                dto.setIdentifiers(Arrays.asList(identifierDto));

            } else if (j == 10) {
                //UAA role
                dto.setUaaRole(cellValue);
            }

            j++;
        }
    }

}
