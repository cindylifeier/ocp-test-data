package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OrganizationHelper {

    private static DataFormatter dataFormatter = new DataFormatter();


    public static void process(Sheet organizations) {
        log.info("last row number : " + organizations.getLastRowNum());
        int rowNum = 0;

        List<OrganizationDto> organizationDtos = new ArrayList<>();

        OrganizationDto orgDto = new OrganizationDto();
        orgDto.setName("Omnibus Care Plan (SAMSHA)");

        orgDto.setAddresses(CommonHelper.getAddresses("5600 Fishers Lane", "Rockville", "MD", "20857"));
        orgDto.setIdentifiers(CommonHelper.getIdentifiers(ConstantsUtil.ORG_TAX_ID_URI, "530196960"));
        List<TelecomDto> telecomDtos = new ArrayList<>();
        telecomDtos.addAll(CommonHelper.getTelecoms("phone", "(240)276-2827"));
        telecomDtos.addAll(CommonHelper.getTelecoms("email", "Kenneth.Salyards@SAMHSA.hhs.gov"));
        orgDto.setTelecoms(telecomDtos);
        organizationDtos.add(orgDto);

        for (Row row : organizations) {
            if (rowNum > 0) {
                int j = 0;
                OrganizationDto dto = new OrganizationDto();
                try {
                    processRow(row, j, dto);
                    organizationDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing a row for organization");
                }
            }
            rowNum++;
        }

        RestTemplate rt = new RestTemplate();

        String fooResourceUrl = DataConstants.serverUrl + "organizations";

        organizationDtos.forEach(organizationDto -> {
            try {
                HttpEntity<OrganizationDto> request = new HttpEntity<>(organizationDto);
                OrganizationDto foo = rt.postForObject(fooResourceUrl, request, OrganizationDto.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private static void processRow(Row row, int j, OrganizationDto dto) {
        String line = "";
        String city = "";
        String state = "";
        String zip = "";
        for (Cell cell : row) {
            String cellValue = dataFormatter.formatCellValue(cell);

            if (j == 0) {
                dto.setName(cellValue);
            } else if (j == 2) {
                line = cellValue.trim();
            } else if (j == 3) {
                city = cellValue.trim();
            } else if (j == 4) {
                state = cellValue.trim();
            } else if (j == 5) {
                zip = cellValue.trim();
                dto.setAddresses(CommonHelper.getAddresses(line, city, state, zip));
            } else if (j == 6) {
                dto.setTelecoms(CommonHelper.getTelecoms("phone", cellValue));
            } else if (j == 7) {
                dto.setIdentifiers(CommonHelper.getIdentifiers(ConstantsUtil.ORG_TAX_ID_URI, cellValue));
            } else if (j == 8) {
                dto.setActive(true);
            }
            j++;
        }
    }
}
