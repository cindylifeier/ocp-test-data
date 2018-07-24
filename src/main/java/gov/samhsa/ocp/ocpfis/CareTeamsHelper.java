package gov.samhsa.ocp.ocpfis;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
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
public class CareTeamsHelper {

    public static void process(Sheet careTeams, Map<String, String> mapOfPractitioners, Map<String, String> mapOfPatients, Map<String, String> mapOrganizations) {
        log.info("last row number : " + careTeams.getLastRowNum());

        int rowNum = 0;

        Map<String, String> careTeamsCategories = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/care-team-categories");
        Map<String, String> careTeamReasonCodes = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/care-team-reasons");
        Map<String, String> participantRoles = CommonHelper.getLookup(DataConstants.serverUrl + "lookups/participant-roles");

        List<CareTeamDto> careTeamDtos = retrieveSheet(careTeams, mapOfPractitioners, mapOfPatients, rowNum, careTeamsCategories, careTeamReasonCodes, participantRoles, mapOrganizations);

        RestTemplate rt = new RestTemplate();


        careTeamDtos.forEach(careTeamDto -> {
            try {
                if (careTeamDto.getSubjectId() != null && careTeamDto.getParticipants() != null) {
                    log.info("Getting ready to post: " + careTeamDto);
                    HttpEntity<CareTeamDto> request = new HttpEntity<>(careTeamDto);
                    rt.postForObject(DataConstants.serverUrl + "care-teams/", request, CareTeamDto.class);
                }
            } catch (Exception e) {
                log.info("This careteam could not be posted : " + careTeamDto);
                e.printStackTrace();

            }

        });

    }

    private static List<CareTeamDto> retrieveSheet(Sheet careTeams, Map<String, String> mapOfPractitioners, Map<String, String> mapOfPatients, int rowNum, Map<String, String> careTeamsCategories, Map<String, String> careTeamReasonCodes,
                                                   Map<String, String> participantRoles, Map<String, String> mapOfOrganizations) {
        List<CareTeamDto> careTeamDtos = new ArrayList<>();

        for (Row row : careTeams) {
            if (rowNum > 0) {
                int j = 0;
                CareTeamDto dto = new CareTeamDto();
                ParticipantDto participantDto = new ParticipantDto();
                try {
                    processRow(mapOfPractitioners, mapOfPatients, careTeamsCategories, careTeamReasonCodes, participantRoles, row, j, dto, participantDto, mapOfOrganizations);
                    careTeamDtos.add(dto);
                } catch (Exception e) {
                    log.error("Error processing a row for careTeams");
                }


            }
            rowNum++;
        }
        return careTeamDtos;
    }

    private static void processRow(Map<String, String> mapOfPractitioners, Map<String, String> mapOfPatients, Map<String, String> careTeamsCategories, Map<String, String> careTeamReasonCodes, Map<String, String> participantRoles, Row row, int j,
                                   CareTeamDto dto, ParticipantDto participantDto, Map<String, String> mapOfOrganizations) {
        List<ParticipantDto> participantDtos = new ArrayList<>();
        for (Cell cell : row) {
            String cellValue = new DataFormatter().formatCellValue(cell);

            if (j == 0) {
                //patient

                String[] name = cellValue.split(" ");

                if (name.length > 1) {
                    dto.setSubjectLastName(name[1].trim());
                    dto.setSubjectFirstName(name[0].trim());
                    dto.setSubjectId(mapOfPatients.get(cellValue));
                }

            } else if (j == 1) {
                //care team name

                dto.setName(cellValue.trim());

            } else if (j == 2) {
                //category

                dto.setCategoryCode(careTeamsCategories.get(cellValue.trim()));
                dto.setCategoryDisplay(cellValue.trim());

            } else if (j == 3) {
                //status

                dto.setStatusCode("active");
                dto.setStatusDisplay("Active");

            } else if (j == 4) {
                //reason
                dto.setReasonCode(careTeamReasonCodes.get(cellValue.trim()));
                dto.setReasonDisplay(cellValue.trim());

            } else if (j == 5) {
                //start date

                dto.setStartDate("01/01/2018");

            } else if (j == 6) {
                //end date

                dto.setEndDate("01/01/2020");

            } else if (j == 7) {
                //participant

                String[] names = cellValue.split(",");
                for (int i = 0; i < names.length; i++) {
                    ParticipantDto participant = new ParticipantDto();
                    participant.setMemberId(mapOfPractitioners.get(names[i]));
                    participant.setMemberLastName(Optional.of(names[i].split(" ")[0]));
                    participant.setMemberFirstName(Optional.of(names[i].split(" ")[1]));
                    participantDtos.add(participant);
                }

            } else if (j == 8) {
                //participant role
                List<ParticipantDto> parts = new ArrayList<>();
                String[] roles = cellValue.split(",");
                int i = 0;
                for (ParticipantDto p : participantDtos) {
                    p.setRoleCode(participantRoles.get(roles[i]));
                    p.setMemberType(participantRoles.get(roles[i]));
                    p.setRoleDisplay(roles[i]);
                    p.setStartDate("01/01/2018");
                    p.setEndDate("01/01/2020");
                    parts.add(p);
                    i++;
                }

                dto.setParticipants(parts);
            } else if (j == 9) {
                //Participant type
                List<ParticipantDto> parts = new ArrayList<>();
                String[] types = cellValue.split(",");
                int i = 0;
                for (ParticipantDto p : participantDtos) {
                    p.setMemberType(participantRoles.get(types[i]));
                    parts.add(p);
                    i++;
                }

                dto.setParticipants(parts);
            } else if (j == 10) {
                //Organization
                dto.setManagingOrganization(mapOfOrganizations.get(cellValue.trim()));
            }

            j++;
        }
    }
}
