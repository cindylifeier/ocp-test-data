package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.model.location.TempLocationDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class LocationsHelper {

    public static void process(Sheet locations, Map<String, String> mapOrganizations) {
        log.info("last row number : " + locations.getLastRowNum());

        int rowNum = 0;

        List<TempLocationDto> locationDtos = new ArrayList<>();

        for (Row row : locations) {
            if (rowNum > 0) {
                int j = 0;
                TempLocationDto dto = new TempLocationDto();
                try {
                    processRow(mapOrganizations, row, j, dto);
                    locationDtos.add(dto);

                } catch (Exception e) {
                    //ignore the row and move on to the next row
                    e.printStackTrace();
                }
            }
            rowNum++;
        }

        RestTemplate rt = new RestTemplate();

        locationDtos.forEach(locationDto -> {
            try {
                HttpEntity<TempLocationDto> request = new HttpEntity<>(locationDto);
                log.info("locationDto being posted : " + locationDto);

                ResponseEntity<TempLocationDto> response = rt.exchange(DataConstants.serverUrl + "organizations/" + locationDto.getManagingOrganization() + "/locations", HttpMethod.POST, request, TempLocationDto.class);
                log.info("response code : " + response.getStatusCode());

            } catch (Exception e) {
                e.printStackTrace();

            }
        });
    }

    private static void processRow(Map<String, String> mapOrganizations, Row row, int j, TempLocationDto dto) {
        for (Cell cell : row) {
            String street="";
            String city="";
            String state="";
            String zip="";
            String cellValue = new DataFormatter().formatCellValue(cell);

            processCell(mapOrganizations, j, dto, street, city, state, zip, cellValue);
            j++;
        }
    }


    private static void processCell(Map<String, String> mapOrganizations, int j, TempLocationDto dto, String street, String city, String state, String zip, String cellValue) {
        if (j == 0) {
            //get the id of the organization
            dto.setManagingOrganization(mapOrganizations.get(cellValue.trim()));

        } else if (j == 1) {
            dto.setName(cellValue);

        } else if (j == 2) {
            street=cellValue.trim();
        } else if (j == 3) {
            city=cellValue.trim();
        }else if (j == 4) {
            state=cellValue.trim();
        }else if (j == 5) {
            zip=cellValue.trim();
            dto.setAddress(CommonHelper.getAddress(street,city,state,zip));
        }
        else if (j == 6) {
            dto.setTelecoms(CommonHelper.getTelecoms("phone", cellValue));

        } else if (j == 8) {
            dto.setIdentifiers(CommonHelper.getIdentifiers("Organization Tax ID", cellValue));

        } else if (j == 9) {
            dto.setStatus("active");
        }
    }


}
